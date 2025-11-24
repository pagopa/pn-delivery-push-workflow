package it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.deliverypushworkflow.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.impl.ReworkRequestEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsReworkRequestEventsProducer extends AbstractSqsMomProducer<ReworkRequestEvent> {

    public SqsReworkRequestEventsProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper ) {
        super(sqsClient, topic, objectMapper, ReworkRequestEvent.class );
    }
}