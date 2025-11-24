package it.pagopa.pn.deliverypushworkflow;

import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.impl.ReworkRequestEvent;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@EnableAutoConfiguration(exclude= {SqsAutoConfiguration.class, ContextFunctionCatalogAutoConfiguration.class})
public abstract class MockAWSObjectsTest extends MockActionPoolTest {

    @MockitoBean
    private MomProducer<ReworkRequestEvent> reworkRequestEventPoolImpl;

    @MockitoBean
    private SqsAsyncClient sqsAsyncClient;

    @MockitoBean
    private DynamoDbClient dynamoDbClient;
}
