package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypushworkflow.action.rework.ReworkHandler;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionType;
import lombok.CustomLog;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class ReworkNotificationHandler extends AbstractActionEventHandler {
    private final ReworkHandler reworkHandler;

    public ReworkNotificationHandler(TimelineUtils timelineUtils, ReworkHandler reworkHandler) {
        super(timelineUtils);
        this.reworkHandler = reworkHandler;
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.NOTIFICATION_REWORK_VALIDATION;
    }

    @Override
    public void handle(Action action, MessageHeaders headers) {
        final String processName = ActionType.NOTIFICATION_REWORK_VALIDATION.name();

        try {
            log.debug("Handle action of type NOTIFICATION_REWORK_VALIDATION, with payload {}", action);
            HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());

            log.logStartingProcess(processName);
            //TODO checkNotificationCancelledAndExecute è necessario?
            checkNotificationCancelledAndExecute(
                    action,
                    a -> reworkHandler.handleRework(a)
            );
            log.logEndingProcess(processName);
        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage());
            HandleEventUtils.handleException(headers, ex);
            throw ex;
        }
    }
}
