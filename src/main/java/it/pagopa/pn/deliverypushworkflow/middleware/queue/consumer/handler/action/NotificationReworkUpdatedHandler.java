package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.action;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypushworkflow.action.rework.ReworkUpdateHandler;
import it.pagopa.pn.deliverypushworkflow.action.rework.ReworkValidationHandler;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkInfo;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionType;
import lombok.CustomLog;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@CustomLog
public class NotificationReworkUpdatedHandler extends AbstractActionEventHandler {
    private final ReworkUpdateHandler reworkUpdateHandler;

    public NotificationReworkUpdatedHandler(TimelineUtils timelineUtils, ReworkUpdateHandler reworkUpdateHandler) {
        super(timelineUtils);
        this.reworkUpdateHandler = reworkUpdateHandler;
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.NOTIFICATION_REWORK_UPDATE;
    }

    @Override
    public void handle(Action action, MessageHeaders headers) {
        final String processName = ActionType.NOTIFICATION_REWORK_UPDATE.name();
        log.debug("Handle action of type NOTIFICATION_REWORK_UPDATE, with payload {}", action);
        HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
        log.logStartingProcess(processName);

        MDCUtils.addMDCToContextAndExecute(Mono.just(action)
                    .flatMap(reworkUpdateHandler::handleNotificationReworkUpdate)
                    .doOnSuccess(resultFromAsync -> log.logEndingProcess(processName))
                    .doOnError(error -> log.logEndingProcess(processName, false, error.getMessage())))
                .block();
    }
}
