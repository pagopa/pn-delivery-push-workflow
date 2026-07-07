package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypushworkflow.action.details.SendCourtesyMessageActionDetails;
import it.pagopa.pn.deliverypushworkflow.action.utils.CourtesyMessageUtils;
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
public class SendCourtesyMessageActionEventHandler extends AbstractActionEventHandler {
    private final CourtesyMessageUtils courtesyMessageUtils;

    public SendCourtesyMessageActionEventHandler(TimelineUtils timelineUtils, CourtesyMessageUtils courtesyMessageUtils) {
        super(timelineUtils);
        this.courtesyMessageUtils = courtesyMessageUtils;
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.SEND_COURTESY_MESSAGE_ACTION;
    }

    @Override
    public void handle(Action action, MessageHeaders headers) {
        final String processName = ActionType.SEND_COURTESY_MESSAGE_ACTION.name();

        try {
            log.debug("Handle action of type SEND_COURTESY_MESSAGE_ACTION, with payload {}", action);
            HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());

            log.logStartingProcess(processName);
            checkNotificationCancelledAndExecute(
                    action,
                    a -> courtesyMessageUtils.handleSendCourtesyMessageAction(a.getIun(), a.getRecipientIndex(), (SendCourtesyMessageActionDetails) a.getDetails())
            );
            log.logEndingProcess(processName);
        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage(), ex);
            HandleEventUtils.handleException(headers, ex);
            throw ex;
        }
    }
}
