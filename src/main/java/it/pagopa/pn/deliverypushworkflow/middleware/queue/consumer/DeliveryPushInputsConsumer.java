package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.router.EventRouter;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import static it.pagopa.pn.deliverypushworkflow.middleware.queue.utils.ChannelUtils.setMdc;

@Configuration
@AllArgsConstructor
@CustomLog
public class DeliveryPushInputsConsumer {
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;
    private final EventRouter eventRouter;

    @SqsListener(queueNames = "#{@pnDeliveryPushWorkflowConfigs.topics.newNotifications}")
    public void pnDeliveryPushInputsInboundConsumer(Message<String> message) {
        setMdc(message);
        final String processName = "DELIVERY_PUSH_INPUTS_INBOUND_CONSUMER";

        try {
            log.info("Handle action pnDeliveryPushInputsInboundConsumer, with content {}", message);

            EventRouter.RoutingConfig routerConfig = EventRouter.RoutingConfig.builder()
                    .deserializePayload(true)
                    .build();
            eventRouter.route(message, routerConfig);
            log.logEndingProcess(processName);
        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage());
            HandleEventUtils.handleException(message.getHeaders(), ex);
            throw ex;
        }
    }
}
