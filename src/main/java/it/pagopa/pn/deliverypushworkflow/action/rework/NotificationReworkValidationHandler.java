package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkValidationDetails;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.rework.NotificationReworkError;
import it.pagopa.pn.deliverypushworkflow.dto.rework.NotificationReworkErrorCause;
import it.pagopa.pn.deliverypushworkflow.dto.rework.NotificationReworkInfo;
import it.pagopa.pn.deliverypushworkflow.exceptions.NotificationReworkValidationException;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.SafeStorageService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReworkValidationHandler {

    private final List<String> MONO_REC_NOTIFICATION_VALID_STATUS = List.of("EFFECTIVE_DATE", "RETURNED_TO_SENDER", "VIEWED");
    private final List<String> MULTI_REC_NOTIFICATION_VALID_STATUS = List.of("DELIVERING", "DELIVERED", "EFFETCTIVE_DATE", "VIEWED", "RETURNED_TO_SENDER", "UNREACHABLE");
    private final String REC_INDEX = "RECINDEX_";

    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final SafeStorageService safeStorageService;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;

    private Mono<NotificationReworkInfo> checkNotificationStatusAndThrow(NotificationReworkInfo info) {
        return Mono.just(notificationService.getNotificationByIun(info.getAction().getIun()))
                .flatMap(notification -> {
                    info.setRecipientSize(notification.getRecipients().size());
                    int recIndex = getRecIndexFromAction(info.getAction());
                    if (notification.getRecipients().size() > recIndex) {
                        return checkNotificationStatus(notification, info);
                    } else {
                        return Mono.error(new NotificationReworkValidationException(NotificationReworkError.builder().cause(NotificationReworkErrorCause.INVALID_RECINDEX.getCause()).description(NotificationReworkErrorCause.INVALID_RECINDEX.getErrorDetails()).build()));
                    }
                });
    }

    private Mono<NotificationReworkInfo> checkNotificationAttachments(NotificationReworkInfo info) {
        return Flux.fromIterable(info.getNotification().getDocuments())
                .flatMap(document -> safeStorageService.getFile(document.getRef().getKey(), true, false))
                .flatMap(response -> {
                    if (response.getRetentionUntil().minusDays(pnDeliveryPushWorkflowConfigs.getNotificationReworkDocumentExpiringRange()).isBefore(OffsetDateTime.now())) {
                        info.getErrorList().add(NotificationReworkError.builder().cause(NotificationReworkErrorCause.INVALID_ATTACHMENT.getCause()).description(String.format(NotificationReworkErrorCause.INVALID_ATTACHMENT.getErrorDetails(), response.getKey(), response.getRetentionUntil())).build());
                    }
                    return Mono.just(info);
                })
                .doOnError(PnHttpResponseException.class, ex -> {
                    if(ex.getStatusCode() == HttpStatus.GONE.value()) {
                        info.getErrorList().add(NotificationReworkError.builder().cause(NotificationReworkErrorCause.EXPIRED_ATTACHMENT.getCause()).description(NotificationReworkErrorCause.EXPIRED_ATTACHMENT.getErrorDetails()).build());
                    }
                })
                .then(Mono.just(info));
    }

    private int getRecIndexFromAction(Action action) {
        String recIndex = ((NotificationReworkValidationDetails) action.getDetails()).getReworkrecIndex();
        return Integer.parseInt(recIndex.substring(recIndex.lastIndexOf(REC_INDEX)) + 1);
    }

    private Mono<NotificationReworkInfo> checkNotificationStatus(NotificationInt notification, NotificationReworkInfo info) {
        NotificationHistoryResponse response = timelineService.getTimelineAndStatusHistory(notification.getIun(), notification.getRecipients().size(), notification.getSentAt());
        info.setNotificationStatus(response.getNotificationStatus().getValue());
        if ((notification.getRecipients().size() == 1 && !MONO_REC_NOTIFICATION_VALID_STATUS.contains(response.getNotificationStatus().getValue())) ||
                (notification.getRecipients().size() > 1 && !MULTI_REC_NOTIFICATION_VALID_STATUS.contains(response.getNotificationStatus().getValue()))) {

            String errorMessage = notification.getRecipients().size() > 1 ?
                    String.format(NotificationReworkErrorCause.INVALID_NOTIFICATION_STATUS.getErrorDetails(), response.getNotificationStatus().getValue(), MULTI_REC_NOTIFICATION_VALID_STATUS) :
                    String.format(NotificationReworkErrorCause.INVALID_NOTIFICATION_STATUS.getErrorDetails(), response.getNotificationStatus().getValue(), MONO_REC_NOTIFICATION_VALID_STATUS);
            return Mono.error(new NotificationReworkValidationException(NotificationReworkError.builder().cause(NotificationReworkErrorCause.INVALID_NOTIFICATION_STATUS.getCause()).description(errorMessage).build()));
        }
        return Mono.just(info);
    }

}
