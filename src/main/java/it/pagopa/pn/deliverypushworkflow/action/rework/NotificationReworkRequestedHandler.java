package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.checkattachmentretention.CheckAttachmentRetentionHandler;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogProgressDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
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

import static it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkConstant.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReworkRequestedHandler {
    private final TimelineService timelineService;
    private final SafeStorageService safeStorageService;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;
    private final CheckAttachmentRetentionHandler checkAttachmentRetentionHandler;
    private final AttachmentUtils attachmentUtils;

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

    private Mono<Void> updateAttachmentRetention(Instant actionCreatedAt, String iun, List<NotificationDocumentInt> documents) {
        int retentionUntilDays = (int) pnDeliveryPushWorkflowConfigs.getTimeParams().getAttachmentTimeToAddAfterExpiration().toDays();
        OffsetDateTime newRetentionDate = OffsetDateTime.now().plusDays(retentionUntilDays);
        return Flux.fromIterable(documents)
                .flatMap(document -> safeStorageService.getFile(document.getRef().getKey(), true, false))
                .filter(response -> newRetentionDate.isAfter(response.getRetentionUntil()))
                .doOnNext(response -> attachmentUtils.changeAttachmentRetention(response.getKey(), retentionUntilDays))
                .doOnNext(response -> checkAttachmentRetentionHandler.scheduleCheckAttachmentRetentionBeforeExpiration(iun, actionCreatedAt))
                .then();
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

    private boolean checkDeliveryDetailCode(TimelineElementInternal elem) {
        if (TimelineElementCategoryInt.SEND_ANALOG_PROGRESS.name().equals(elem.getCategory().name())) {
            return !((SendAnalogProgressDetailsInt) elem.getDetails()).getDeliveryDetailCode().startsWith(CON);
        }
        return true;
    }
}