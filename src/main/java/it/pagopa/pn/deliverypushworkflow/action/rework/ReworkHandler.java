package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkValidationDetails;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.rework.NotificationReworkError;
import it.pagopa.pn.deliverypushworkflow.dto.rework.NotificationReworkErrorCause;
import it.pagopa.pn.deliverypushworkflow.dto.rework.NotificationReworkInfo;
import it.pagopa.pn.deliverypushworkflow.exceptions.NotificationReworkValidationException;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.api.TimelineControllerApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private Mono<NotificationReworkInfo> checkNotificationStatusAndThrow(NotificationReworkInfo info) {
        return Mono.just(notificationService.getNotificationByIun(info.getAction().getIun()))
                .flatMap(notification -> {
                    int recIndex = Integer.parseInt(((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkrecIndex());
                    if (notification.getRecipients().size() >= recIndex) {
                        return checkNotificationStatus(notification, recIndex, info);
                    } else {
                        return Mono.error(new NotificationReworkValidationException(NotificationReworkError.builder().cause(NotificationReworkErrorCause.INVALID_RECINDEX.getCause()).description(NotificationReworkErrorCause.INVALID_RECINDEX.getErrorDetails()).build()));
                    }
                });
    }

    private Mono<NotificationReworkInfo> checkNotificationStatus(NotificationInt notification, int recIndex, NotificationReworkInfo info) {
        NotificationHistoryResponse response = timelineControllerApi.getTimelineAndStatusHistory(notification.getIun(), notification.getRecipients().size(), notification.getSentAt());
        if ((recIndex == 1 && !MONO_ACCEPTED_STATUSES.contains(response.getNotificationStatus().getValue())) ||
                (recIndex > 1 && !MULTI_ACCEPTED_STATUSES.contains(response.getNotificationStatus().getValue()))) {

            String errorMessage = recIndex > 1 ?
                    String.format(NotificationReworkErrorCause.INVALID_NOTIFICATION_STATUS.getErrorDetails(), response.getNotificationStatus().getValue(), MULTI_ACCEPTED_STATUSES) :
                    String.format(NotificationReworkErrorCause.INVALID_NOTIFICATION_STATUS.getErrorDetails(), response.getNotificationStatus().getValue(), MONO_ACCEPTED_STATUSES);
            return Mono.error(new NotificationReworkValidationException(NotificationReworkError.builder().cause(NotificationReworkErrorCause.INVALID_NOTIFICATION_STATUS.getCause()).description(errorMessage).build()));
        }
        return Mono.just(info);
    }

}
