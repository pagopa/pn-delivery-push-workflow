package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.checkattachmentretention.CheckAttachmentRetentionHandler;
import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkRequestedDetails;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
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
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

    public Mono<Void> handleNotificationReworkRequested(Action action) {
        NotificationReworkRequestedDetails detail = (NotificationReworkRequestedDetails) action.getDetails();
        List<String> timelineElementsToInvalidate = new ArrayList<>();
        NotificationInt notificationInt = notificationService.getNotificationByIun(action.getIun());
        Set<TimelineElementInternal> timelineElements = timelineService.getTimeline(action.getIun(), true);
        return Mono.just(timelineElements)
                .flatMap(timeline -> computeTimelineElementToInvalidate(timeline, detail.getReworkRecIndex(), detail.getReworkAttempt()))
                .doOnNext(timelineElementsToInvalidate::addAll)
                .flatMap(timelineElementIds -> startNotificationReworkProcess(detail).thenReturn(timelineElementIds))
                .flatMap(strings -> updateAttachmentRetention(detail.getCreatedAt(), notificationInt.getIun(), notificationInt.getDocuments(), detail.getReworkAttempt()))
                .map(internalAction -> buildTimelineElement(notificationInt, timelineElementsToInvalidate, detail))
                .onErrorResume(throwable -> {
                        log.error("Errors during handleNotificationReworkRequested for iun {}: {}", action.getIun(), throwable.getMessage(), throwable);
                        reworkRequestEventPool.scheduleFutureAction(NotificationReworkUtils.getReworkRequestEventAction(throwable.getMessage(), detail, action), ReworkRequestEventType.NOTIFICATION_REWORK_REQUESTED);
                        return Mono.empty();
                })
                .map(timelineElementInternal -> timelineService.addTimelineElement(timelineElementInternal, notificationInt))
                .then();
    }

    private Mono<List<String>> computeTimelineElementToInvalidate(Set<TimelineElementInternal> timelineElementInternalList, String recIndex, String attemptId) {
        log.debug("Starting computeTimelineElementToInvalidate for recIndex {} and attemptId {}", recIndex, attemptId);
        return Flux.fromIterable(timelineElementInternalList)
                .filter(elem -> pnDeliveryPushWorkflowConfigs.getInvalidableCategories().contains(elem.getCategory().name()))
                .filter(elem -> elem.getElementId().contains(recIndex))
                .filter(elem -> checkAttemptId(elem, attemptId))
                .filter(elem -> checkPrepareAnalogDomicile(elem, attemptId))
                .filter(elem -> checkSendAnalogDomicile(elem, attemptId))
                .filter(this::checkDeliveryDetailCode)
                .map(TimelineElementInternal::getElementId)
                .collectList()
                .doOnNext(list -> log.debug("Invalidable elements found: {}", list));
    }

    private Mono<String> updateAttachmentRetention(Instant actionCreatedAt, String iun, List<NotificationDocumentInt> documents, String reworkAttempt) {
        if(reworkAttempt.equalsIgnoreCase(ATTEMPT_0)) {
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
        return Mono.just(iun);
    }

    private TimelineElementInternal buildTimelineElement(NotificationInt notification,  List<String> elementsToInvalidate, NotificationReworkRequestedDetails internalDetail) {
        Integer recIndex = Objects.nonNull(internalDetail.getReworkRecIndex().split("_")[1]) ? Integer.parseInt(internalDetail.getReworkRecIndex().split("_")[1]) : null;
        Integer attempt = Objects.nonNull(internalDetail.getReworkAttempt().split("_")[1]) ? Integer.parseInt(internalDetail.getReworkAttempt().split("_")[1]) : null;
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


    private boolean checkPrepareAnalogDomicile(TimelineElementInternal elem, String attempt) {
        if (ATTEMPT_1.equals(attempt)) {
            return !TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE.equals(elem.getCategory());
        } else {
            return !TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE.equals(elem.getCategory()) || elem.getElementId().contains(ATTEMPT_1);
        }
    }


    private boolean checkSendAnalogDomicile(TimelineElementInternal elem, String attempt) {
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

    private boolean checkDeliveryDetailCode(TimelineElementInternal elem) {
        if (TimelineElementCategoryInt.SEND_ANALOG_PROGRESS.name().equals(elem.getCategory().name())) {
            return !((SendAnalogProgressDetailsInt) elem.getDetails()).getDeliveryDetailCode().startsWith(CON);
        }
        return true;
    }

    public Mono<Void> startNotificationReworkProcess(NotificationReworkRequestedDetails details) {
        log.info("Starting rework process for reworkRequestId {} and reworkId {}", details.getReworkRequestId(), details.getReworkId());
        return  Mono.fromRunnable(() -> paperChannelService.initNotificationRework(details.getReworkRequestId(), details.getReworkId()));
    }
}