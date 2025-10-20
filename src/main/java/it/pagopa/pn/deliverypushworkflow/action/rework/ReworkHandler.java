package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkValidationDetails;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.rework.ReworkError;
import it.pagopa.pn.deliverypushworkflow.dto.rework.ReworkErrorCause;
import it.pagopa.pn.deliverypushworkflow.dto.rework.ReworkInfo;
import it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushWorkflowGenericException;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.api.TimelineControllerApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReworkHandler {

    private final List<String> MONO_ACCEPTED_STATUSES = List.of("EFFECTIVE_DATE", "RETURNED_TO_SENDER", "VIEWED");
    private final List<String> MULTI_ACCEPTED_STATUSES = List.of("DELIVERING", "DELIVERED", "EFFETCTIVE_DATE", "VIEWED", "RETURNED_TO_SENDER", "UNREACHABLE");

    private final NotificationService notificationService;
    private final TimelineControllerApi timelineControllerApi;

    public void handleRework(Action action) {
        log.info("Start handleRework - iun {} id {}", action.getIun(), action.getRecipientIndex());
        ReworkInfo reworkInfo = new ReworkInfo();
        reworkInfo.setAction(action);

        Mono.just(reworkInfo)
                .flatMap(this::checkNotificationCancelledAndThrow)
                .flatMap(this::checkNotificationStatusAndThrow)
                .flatMap(this::retrieveTimeline)
                .flatMap(this::checkNotificationTimelineAndThrow)
                .flatMap(this::checkNotificationExpectedFinalStatusCodeAndThrow)
                .flatMap(this::checkNotificationAttachments)
                .flatMap(this::computeRequestId)
                .flatMap(this::checkNotificationAddress)
                .flatMap(this::checkErrorList)
                .doOnError(e -> {
                    log.error("Errore durante handleRework per iun {}: {}", action.getIun(), e.getMessage(), e);
                    this.checkErrorList(reworkInfo);
                })
                .doOnSuccess(v -> log.info("handleRework completato per iun {}", action.getIun()))
                .block();

    }

    private Mono<ReworkInfo> checkNotificationCancelledAndThrow(ReworkInfo externalInfo) {
        return Mono.just(externalInfo);
    }

    private Mono<ReworkInfo> checkNotificationStatusAndThrow(ReworkInfo info) {
        return Mono.just(notificationService.getNotificationByIun(info.getAction().getIun()))
                .flatMap(notification -> {
                    int recIndex = Integer.parseInt(((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkrecIndex());
                    if (notification.getRecipients().size() >= recIndex) {
                        return checkNotificationStatus(notification, recIndex, info);
                    } else {
                        info.getErrorList().add(ReworkError.builder().cause(ReworkErrorCause.INVALID_RECINDEX.getCause()).description(ReworkErrorCause.INVALID_RECINDEX.getErrorDetails()).build());
                        return Mono.error(new PnDeliveryPushWorkflowGenericException(ReworkErrorCause.INVALID_RECINDEX.getErrorDetails(), HttpStatus.INTERNAL_SERVER_ERROR.toString()));
                    }
                });
    }

    private Mono<ReworkInfo> checkNotificationStatus(NotificationInt notification, int recIndex, ReworkInfo info) {
        NotificationHistoryResponse response = timelineControllerApi.getTimelineAndStatusHistory(notification.getIun(), notification.getRecipients().size(), notification.getSentAt());
        if ((recIndex == 1 && !MONO_ACCEPTED_STATUSES.contains(response.getNotificationStatus().getValue())) ||
                (recIndex > 1 && !MULTI_ACCEPTED_STATUSES.contains(response.getNotificationStatus().getValue()))) {

            String errorMessage = recIndex > 1 ?
                    String.format(ReworkErrorCause.INVALID_NOTIFICATION_STATUS.getErrorDetails(), response.getNotificationStatus().getValue(), MULTI_ACCEPTED_STATUSES) :
                    String.format(ReworkErrorCause.INVALID_NOTIFICATION_STATUS.getErrorDetails(), response.getNotificationStatus().getValue(), MONO_ACCEPTED_STATUSES);

            info.getErrorList().add(ReworkError.builder().cause(ReworkErrorCause.INVALID_NOTIFICATION_STATUS.getCause()).description(errorMessage).build());
            return Mono.error(new PnDeliveryPushWorkflowGenericException(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR.toString()));
        }
        return Mono.just(info);
    }

    private Mono<ReworkInfo> retrieveTimeline(ReworkInfo info) {
        return Mono.just(info);
    }

    private Mono<ReworkInfo> checkNotificationTimelineAndThrow(ReworkInfo info) { return Mono.just(info);}

    private Mono<ReworkInfo> checkNotificationExpectedFinalStatusCodeAndThrow(ReworkInfo info) { return Mono.just(info);}

    private Mono<ReworkInfo> checkNotificationAttachments(ReworkInfo info) { return Mono.just(info);}

    private Mono<ReworkInfo> computeRequestId(ReworkInfo info) {
        return Mono.just(info);
    }

    private Mono<ReworkInfo> checkNotificationAddress(ReworkInfo externalInfo) {
        return Mono.just(externalInfo);
    }

    private Mono<Void> checkErrorList(ReworkInfo externalInfo) {
        return Mono.empty();
    }

}
