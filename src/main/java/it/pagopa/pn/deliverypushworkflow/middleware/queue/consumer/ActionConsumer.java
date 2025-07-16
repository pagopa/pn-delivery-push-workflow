package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer;


import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import static it.pagopa.pn.deliverypushworkflow.middleware.queue.utils.ChannelUtils.setMdc;


@Configuration
@Slf4j
@RequiredArgsConstructor
public class ActionConsumer {
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;

    @SqsListener(queueNames = "#{@pnDeliveryPushWorkflowConfigs.topics.scheduledActions}")
    public void pnDeliveryPushActionsInboundConsumer(Message<String> message) {
        setMdc(message);
        try {
            log.info("messaggio ricevuto {}", message);
            //Todo: to be implemented
        } catch (Exception ex) {
            log.error("Error processing message from {}: {}", pnDeliveryPushWorkflowConfigs, ex.getMessage(), ex);
            throw ex;
        }
    }
}
