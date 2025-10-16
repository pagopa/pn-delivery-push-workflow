package it.pagopa.pn.deliverypushworkflow.middleware.queue.utils;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.deliverypushworkflow.sqs.SqsReworkRequestEventsProducer;
import it.pagopa.pn.stream.middleware.queue.producer.stream.sqs.SqsSortEventsProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class PnDeliveryPushWorkflowMiddlewareConfigs {

    private final PnDeliveryPushWorkflowConfigs cfg;

    public PnDeliveryPushWorkflowMiddlewareConfigs(PnDeliveryPushWorkflowConfigs cfg) {
        this.cfg = cfg;
    }

    @Bean
    public SqsReworkRequestEventsProducer reworkRequestEventsProducer(SqsClient sqs, ObjectMapper objMapper) {
        return new SqsReworkRequestEventsProducer( sqs, cfg.getTopics().getReworkRequestEvents(), objMapper);
    }
}

