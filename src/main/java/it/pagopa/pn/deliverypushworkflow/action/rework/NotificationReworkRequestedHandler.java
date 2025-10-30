package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.checkattachmentretention.CheckAttachmentRetentionHandler;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogProgressDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.PaperChannelService;
import it.pagopa.pn.deliverypushworkflow.service.SafeStorageService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkConstant.ATTEMPT_0;
import static it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkConstant.ATTEMPT_1;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReworkRequestedHandler {

    private final TimelineService timelineService;
    private final NotificationService notificationService;
    private final PaperChannelService paperChannelService;
    private final SafeStorageService safeStorageService;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;
    private final CheckAttachmentRetentionHandler checkAttachmentRetentionHandler;
    private final AttachmentUtils attachmentUtils;


    public Mono<Void> handleNotificationReworkRequested(Action action) {
        return Mono.just(action)
                .flatMap(this::computeTimelineElementToInvalidate)
                .flatMap(this::startNotificationReworkProcess)
                .flatMap(this::updateAttachmentRetention)
                .flatMap(this::buildTimelineElement)
                .zipWith(Mono.just(notificationService.getNotificationByIun(action.getIun())))
                .flatMap(tuple -> addTimelineElement(tuple.getT1(), tuple.getT2()));
    }

    private Mono<List<String>> computeTimelineElementToInvalidate(Set<TimelineElementInternal> timelineElementInternalList, String recIndex, String attemptId) {
        return Flux.fromIterable(timelineElementInternalList)
                .filter(elem -> pnDeliveryPushWorkflowConfigs.getInvalidableCategories().contains(elem.getCategory().name()))
                .filter(elem -> elem.getElementId().contains(recIndex))
                .filter(elem -> checkAttemptId(elem, attemptId))
                .filter(elem -> checkPrepareAnalogDomicile(elem, attemptId))
                .filter(this::checkDeliveryDetailCode)
                .map(TimelineElementInternal::getElementId)
                .collectList();
    }

    private Mono<Void> updateAttachmentRetention(Instant actionCreatedAt, String iun, List< NotificationDocumentInt > documents) {
        int retentionUntilDays = (int) pnDeliveryPushWorkflowConfigs.getTimeParams().getAttachmentTimeToAddAfterExpiration().toDays();
        OffsetDateTime newRetentionDate = OffsetDateTime.now().plusDays(retentionUntilDays);
        return Flux.fromIterable(documents)
                .flatMap(document -> safeStorageService.getFile(document.getRef().getKey(), true, false))
                .filter(response -> newRetentionDate.isAfter(response.getRetentionUntil()))
                .flatMap(response -> attachmentUtils.changeAttachmentRetention(response.getKey(), newRetentionDate))
                .collectList()
                .doOnNext(response -> checkAttachmentRetentionHandler.scheduleCheckAttachmentRetentionBeforeExpiration(iun, actionCreatedAt))
                .then();
    }

    private Mono<Void> addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        return Mono.just(timelineService.addTimelineElement(element, notification)).then();
    }

    private Mono<TimelineElementInternal> buildTimelineElement(Action action) {
        //TODO da implementare quando sarà disponibile la logica
        return Mono.just(new TimelineElementInternal());
    }


    private boolean checkPrepareAnalogDomicile(TimelineElementInternal elem, String attempt) {
        if (ATTEMPT_0.equals(attempt)) {
            return !TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE.name().equals(elem.getCategory().name()) && elem.getElementId().contains(ATTEMPT_0);
        } else {
            return !TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE.name().equals(elem.getCategory().name()) && elem.getElementId().contains(ATTEMPT_1);
        }
    }

    private boolean checkAttemptId(TimelineElementInternal elem, String attempt) {
        if (ATTEMPT_1.equals(attempt)) {
            return !elem.getElementId().contains(ATTEMPT_0);
        }
        return true;
    }

    public Mono<Action> computeTimelineElementToInvalidate(Action action) {
        return Mono.just(action);
    }

    private boolean checkDeliveryDetailCode(TimelineElementInternal elem) {
        if (TimelineElementCategoryInt.SEND_ANALOG_PROGRESS.name().equals(elem.getCategory().name())) {
            return !((SendAnalogProgressDetailsInt) elem.getDetails()).getDeliveryDetailCode().startsWith(CON);
        }
        return true;
    }

    public Mono<Action> startNotificationReworkProcess(Action action) {
        NotificationReworkRequestedDetails details = (NotificationReworkRequestedDetails) action.getDetails();
        return Mono.just(paperChannelService.initNotificationRework(details.getReworkrequestId(), details.getReworkId())).thenReturn(action);
    }
}