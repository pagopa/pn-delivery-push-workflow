package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.action;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypushworkflow.action.rework.ReworkRequestedHandler;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
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
public class NotificationReworkRequestedHandler extends AbstractActionEventHandler {
    private final ReworkRequestedHandler notificationReworkRequestedService;

    public NotificationReworkRequestedHandler(TimelineUtils timelineUtils, ReworkRequestedHandler notificationReworkRequestedService) {
        super(timelineUtils);
        this.notificationReworkRequestedService = notificationReworkRequestedService;
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.NOTIFICATION_REWORK_REQUESTED;
    }

    @Override
    public void handle(Action action, MessageHeaders headers) {
        final String processName = ActionType.NOTIFICATION_REWORK_REQUESTED.name();
        log.debug("Handle action of type NOTIFICATION_REWORK_REQUESTED, with payload {}", action);
        HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
        log.logStartingProcess(processName);

        MDCUtils.addMDCToContextAndExecute(Mono.just(action)
                        .flatMap(notificationReworkRequestedService::handleNotificationReworkRequested)
                        .doOnSuccess(resultFromAsync -> log.logEndingProcess(processName))
                        .doOnError(error -> log.logEndingProcess(processName, false, error.getMessage())))
                .block();
    }
}
