package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.checkattachmentretention.CheckAttachmentRetentionHandler;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypushworkflow.service.SafeStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReworkRequestedHandler {
    private final SafeStorageService safeStorageService;
    private final PnDeliveryPushWorkflowConfigs configs;
    private final CheckAttachmentRetentionHandler checkAttachmentRetentionHandler;
    private final AttachmentUtils attachmentUtils;

    private Mono<Void> updateAttachmentRetention(Instant actionCreatedAt, String iun, List<NotificationDocumentInt> documents) {
        int retentionUntilDays = (int) configs.getTimeParams().getAttachmentTimeToAddAfterExpiration().toDays();
        OffsetDateTime newRetentionDate = OffsetDateTime.now().plusDays(retentionUntilDays);
        return Flux.fromIterable(documents)
                .flatMap(document -> safeStorageService.getFile(document.getRef().getKey(), true, false))
                .filter(response -> newRetentionDate.isAfter(response.getRetentionUntil()))
                .doOnNext(response -> attachmentUtils.changeAttachmentRetention(response.getKey(), retentionUntilDays))
                .doOnNext(response -> checkAttachmentRetentionHandler.scheduleCheckAttachmentRetentionBeforeExpiration(iun, actionCreatedAt))
                .then();
    }
}