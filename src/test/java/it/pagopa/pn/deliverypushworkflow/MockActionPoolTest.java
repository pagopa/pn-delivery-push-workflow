package it.pagopa.pn.deliverypushworkflow;

import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionsPool;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public abstract class MockActionPoolTest {
    @MockitoBean
    private ActionsPool actionsPool;
}
