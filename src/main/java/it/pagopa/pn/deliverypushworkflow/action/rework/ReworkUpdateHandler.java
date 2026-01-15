package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkUpdateDetails;
import it.pagopa.pn.deliverypushworkflow.action.utils.NotificationReworkUtils;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkError;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkErrorCause;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkOperationEnum;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.UpdateValidationStatusEnum;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventAction;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventPool;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventType;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReworkUpdateHandler {

    private final TimelineService timelineService;
    private final ReworkRequestEventPool reworkRequestEventPool;

    public Mono<Void> handleNotificationReworkUpdate(Action action) {
        log.info("Start handleUpdateRework - iun {} id {}", action.getIun(), action.getRecipientIndex());

        return this.retrieveTimeline(action.getIun())
                .map(timeline -> this.checkNotificationExpectedFinalStatusCodeAndThrow(timeline, (NotificationReworkUpdateDetails) action.getDetails()))
                .doOnNext(errorList -> reworkRequestEventPool.scheduleFutureAction(getReworkRequestEventAction(errorList, (NotificationReworkUpdateDetails) action.getDetails(), action), ReworkRequestEventType.NOTIFICATION_REWORK_REQUESTED))
                .doOnError(throwable -> log.error("Error inserting update action for iun {}: {}", action.getIun(), throwable.getMessage()))
                .then();
    }

    private Mono<Set<TimelineElementInternal>> retrieveTimeline(String iun) {
        return Mono.just(timelineService.getTimeline(iun, false));
    }

    private List<NotificationReworkError> checkNotificationExpectedFinalStatusCodeAndThrow(Set<TimelineElementInternal> timeline, NotificationReworkUpdateDetails detail) {
        return NotificationReworkUtils.checkNotificationExpectedFinalStatusCodeAndThrow(
                detail.getReworkAttempt(),
                detail.getReworkExpectedFinalStatus(),
                detail.getReworkRecIndex(),
                timeline
        ) ? List.of() :
                List.of(NotificationReworkError.builder()
                        .cause(NotificationReworkErrorCause.INVALID_EXPECTED_STATUS_CODE.getCause())
                        .description(String.format(NotificationReworkErrorCause.INVALID_EXPECTED_STATUS_CODE.getErrorDetails(), detail.getReworkExpectedFinalStatus(), detail.getReworkAttempt()))
                        .build());
    }

    private ReworkRequestEventAction getReworkRequestEventAction(List<NotificationReworkError> errorList, NotificationReworkUpdateDetails detail, Action action) {
        ReworkRequestEventAction reworkRequest = new ReworkRequestEventAction();
        reworkRequest.setIun(action.getIun());
        reworkRequest.setReworkId(detail.getReworkId());
        reworkRequest.setExpectedStatusCodes(detail.getReworkExpectedStatusCodes());
        reworkRequest.setExpectedDeliveryFailureCause(detail.getReworkExpectedDeliveryFailureCause());
        if (!CollectionUtils.isEmpty(errorList)) {
            reworkRequest.setUpdateValidationStatus(UpdateValidationStatusEnum.KO.name());
            reworkRequest.setError(errorList);
        } else {
            reworkRequest.setUpdateValidationStatus(UpdateValidationStatusEnum.OK.name());
        }
        reworkRequest.setOperation(NotificationReworkOperationEnum.UPDATE_REQUEST.name());
        return reworkRequest;
    }
}