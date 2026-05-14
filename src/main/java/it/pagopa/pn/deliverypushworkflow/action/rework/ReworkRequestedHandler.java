package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.checkattachmentretention.CheckAttachmentRetentionHandler;
import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkRequestedDetails;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.ReworkRequestTypeEnum;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogProgressDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationStatusHistoryElement;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.utils.NotificationReworkUtils;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventPool;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventType;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.PaperChannelService;
import it.pagopa.pn.deliverypushworkflow.service.SafeStorageService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

import static it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkConstant.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReworkRequestedHandler {

    private final TimelineService timelineService;
    private final NotificationService notificationService;
    private final PaperChannelService paperChannelService;
    private final SafeStorageService safeStorageService;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;
    private final CheckAttachmentRetentionHandler checkAttachmentRetentionHandler;
    private final AttachmentUtils attachmentUtils;
    private final TimelineUtils timelineUtils;
    private final ReworkRequestEventPool reworkRequestEventPool;

    public Mono<Void> handleNotification(Action action) {
        NotificationReworkRequestedDetails detail = (NotificationReworkRequestedDetails) action.getDetails();
        if (Objects.isNull(detail.getRequestType())) {
            return Mono.error(new IllegalArgumentException("Request type is required for rework request with reworkId " + detail.getReworkId() + " and iun " + action.getIun()));
        }

        if (ReworkRequestTypeEnum.RESTART.name().equals(detail.getRequestType().name())) {
            return handleNotificationRestart(action, detail);
        } else {
            return handleNotificationRework(action, detail);
        }
    }

    private Mono<Void> handleNotificationRework(Action action, NotificationReworkRequestedDetails detail) {
        return initializeReworkRequest(action, detail)
                .then();
    }

    private Mono<Void> handleNotificationRestart(Action action, NotificationReworkRequestedDetails detail) {
        Integer recIndex = extractTimelineIndex(detail.getReworkRecIndex(), "reworkRecIndex");
        Integer attempt = extractTimelineIndex(detail.getReworkAttempt(), "reworkAttempt");
        return initializeReworkRequest(action, detail)
                .doOnNext(notificationInt -> paperChannelService.prepareAnalogNotification(notificationInt, recIndex, attempt))
                .then();
    }

    private Mono<NotificationInt> initializeReworkRequest(Action action, NotificationReworkRequestedDetails detail) {
        NotificationInt notificationInt = notificationService.getNotificationByIun(action.getIun());
        Set<TimelineElementInternal> timelineElements = timelineService.getTimeline(action.getIun(), true);
        List<String> timelineElementsToInvalidate = new ArrayList<>();

        return Mono.just(timelineElements)
                .flatMap(timeline -> computeTimelineElementToInvalidate(timeline, detail.getReworkRecIndex(), detail.getReworkAttempt(), detail.getRequestType()))
                .doOnNext(timelineElementsToInvalidate::addAll)
                .flatMap(timelineElementIds -> startNotificationReworkProcess(detail).thenReturn(timelineElementIds))
                .flatMap(strings -> updateAttachmentRetention(detail.getCreatedAt(), notificationInt.getIun(), notificationInt.getDocuments()))
                .map(internalAction -> buildTimelineElement(notificationInt, timelineElementsToInvalidate, detail))
                .map(timelineElementInternal -> timelineService.addTimelineElement(timelineElementInternal, notificationInt))
                .map(ignore -> notificationInt)
                .onErrorResume(throwable -> {
                    log.error("Errors during handleNotificationReworkRequested for iun {}: {}", action.getIun(), throwable.getMessage(), throwable);
                    reworkRequestEventPool.scheduleFutureAction(NotificationReworkUtils.getReworkRequestEventAction(throwable.getMessage(), detail, action), ReworkRequestEventType.NOTIFICATION_REWORK_REQUESTED);
                    return Mono.empty();
                });
    }

    private Mono<List<String>> computeTimelineElementToInvalidate(Set<TimelineElementInternal> timelineElementInternalList, String recIndex, String attemptId, ReworkRequestTypeEnum requestType) {
        log.debug("Starting computeTimelineElementToInvalidate for recIndex {} and attemptId {}", recIndex, attemptId);
        if (Objects.isNull(requestType)) {
            return Mono.error(new IllegalArgumentException("Request type is required to compute timeline elements to invalidate"));
        }

        return Flux.fromIterable(timelineElementInternalList)
                .filter(elem -> pnDeliveryPushWorkflowConfigs.getInvalidableCategories().contains(elem.getCategory().name()))
                .filter(elem -> elem.getElementId().contains(recIndex))
                .filter(elem -> checkAttemptId(elem, attemptId))
                .filter(elem -> checkPrepareAnalogDomicile(elem, attemptId, requestType))
                .filter(elem -> checkSendAnalogDomicile(elem, attemptId, requestType))
                .filter(timelineElementInternal -> checkDeliveryDetailCode(timelineElementInternal, attemptId, requestType))
                .map(TimelineElementInternal::getElementId)
                .collectList()
                .doOnNext(list -> log.debug("Invalidable elements found: {}", list));
    }

    private Mono<String> updateAttachmentRetention(Instant actionCreatedAt, String iun, List<NotificationDocumentInt> documents) {
        log.debug("Starting updateAttachmentRetention for iun {} with {} documents", iun, documents.size());
        int retentionUntilDays = (int) pnDeliveryPushWorkflowConfigs.getTimeParams().getAttachmentTimeToAddAfterExpiration().toDays();
        OffsetDateTime newRetentionDate = OffsetDateTime.now().plusDays(retentionUntilDays);
        return Flux.fromIterable(documents)
                .flatMap(document -> safeStorageService.getFile(document.getRef().getKey(), true, false))
                .filter(response -> newRetentionDate.isAfter(response.getRetentionUntil()))
                .flatMap(response -> {
                    log.info("Updating retention for file {}: new date {}", response.getKey(), newRetentionDate);
                    return attachmentUtils.changeAttachmentRetention(response.getKey(), newRetentionDate);
                })
                .collectList()
                .doOnNext(response -> {
                    log.debug("Retention updated for iun {}. Scheduling checkAttachmentRetention.", iun);
                    checkAttachmentRetentionHandler.scheduleCheckAttachmentRetentionBeforeExpiration(iun, actionCreatedAt);
                })
                .thenReturn(iun);
    }

    private TimelineElementInternal buildTimelineElement(NotificationInt notification,  List<String> elementsToInvalidate, NotificationReworkRequestedDetails internalDetail) {
        Integer recIndex = extractTimelineIndex(internalDetail.getReworkRecIndex(), "reworkRecIndex");
        Integer attempt = extractTimelineIndex(internalDetail.getReworkAttempt(), "reworkAttempt");
        NotificationHistoryResponse notificationHistoryResponse = timelineService.getTimelineAndStatusHistory(notification.getIun(), notification.getRecipients().size(), notification.getSentAt());
        List<NotificationStatusHistoryElement> statusHistoryElements = new ArrayList<>();
        if(Objects.nonNull(notificationHistoryResponse.getNotificationStatusHistory())) {
            statusHistoryElements = notificationHistoryResponse.getNotificationStatusHistory()
                    .stream()
                    .peek(notificationStatusHistoryElement -> {
                        List<String> filteredRelatedTimelineElements = notificationStatusHistoryElement.getRelatedTimelineElements()
                                .stream().filter(elementsToInvalidate::contains).toList();
                        notificationStatusHistoryElement.setRelatedTimelineElements(filteredRelatedTimelineElements);
                    })
                    .filter(element -> !CollectionUtils.isEmpty(element.getRelatedTimelineElements()))
                    .toList();
        }
        return timelineUtils.buildNotificationTimelineReworkedTimelineElement(notification, statusHistoryElements, recIndex, attempt, internalDetail.getReworkId());
    }

    private Integer extractTimelineIndex(String timelineIndex, String fieldName) {
        String indexValue = StringUtils.substringAfterLast(timelineIndex, "_");
        if (!StringUtils.isNumeric(indexValue)) {
            throw new IllegalArgumentException(String.format("Invalid %s value '%s' for rework request, expected format 'PREFIX_number'", fieldName, timelineIndex));
        }
        return Integer.parseInt(indexValue);
    }


    private boolean checkPrepareAnalogDomicile(TimelineElementInternal elem, String attempt, ReworkRequestTypeEnum requestType) {
        if (TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE.equals(elem.getCategory()) && ReworkRequestTypeEnum.RESTART.name().equals(requestType.name())) {
            return true;
        }

        if (ATTEMPT_1.equals(attempt)) {
            return !TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE.equals(elem.getCategory());
        } else {
            return !TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE.equals(elem.getCategory()) || elem.getElementId().contains(ATTEMPT_1);
        }
    }


    private boolean checkSendAnalogDomicile(TimelineElementInternal elem, String attempt, ReworkRequestTypeEnum requestType) {
        if (TimelineElementCategoryInt.SEND_ANALOG_DOMICILE.equals(elem.getCategory()) && ReworkRequestTypeEnum.RESTART.name().equals(requestType.name())) {
            return true;
        }

        if (ATTEMPT_1.equals(attempt)) {
            return !TimelineElementCategoryInt.SEND_ANALOG_DOMICILE.equals(elem.getCategory());
        } else {
            return !TimelineElementCategoryInt.SEND_ANALOG_DOMICILE.equals(elem.getCategory()) || elem.getElementId().contains(ATTEMPT_1);
        }
    }

    private boolean checkAttemptId(TimelineElementInternal elem, String attempt) {
        if (ATTEMPT_1.equals(attempt)) {
            return !elem.getElementId().contains(ATTEMPT_0);
        }
        return true;
    }

    private boolean checkDeliveryDetailCode(TimelineElementInternal elem, String attemptId, ReworkRequestTypeEnum requestType) {

        if (elem.getCategory() != TimelineElementCategoryInt.SEND_ANALOG_PROGRESS) {
            return true;
        }

        String elementId = elem.getElementId();
        SendAnalogProgressDetailsInt details = (SendAnalogProgressDetailsInt) elem.getDetails();
        boolean isAttempt0 = ATTEMPT_0.equals(attemptId);
        boolean isAttempt1 = ATTEMPT_1.equals(attemptId);

        if (isAttempt0 && elementId.contains(ATTEMPT_1)) {
            return true;
        }

        if ((isAttempt0 && elementId.contains(ATTEMPT_0)) || (isAttempt1 && elementId.contains(ATTEMPT_1))) {
            return ReworkRequestTypeEnum.RESTART.name().equals(requestType.name()) || !details.getDeliveryDetailCode().startsWith(CON);
        }

        return true;
    }


    public Mono<Void> startNotificationReworkProcess(NotificationReworkRequestedDetails details) {
        if (ReworkRequestTypeEnum.RESTART.equals(details.getRequestType())) {
            log.debug("Request type is RESTART, skipping paper channel rework process for reworkRequestId {} and reworkId {}", details.getReworkRequestId(), details.getReworkId());
            return Mono.empty();
        }
        log.info("Starting rework process for reworkRequestId {} and reworkId {}", details.getReworkRequestId(), details.getReworkId());
        return  Mono.fromRunnable(() -> paperChannelService.initNotificationRework(details.getReworkRequestId(), details.getReworkId()));
    }
}