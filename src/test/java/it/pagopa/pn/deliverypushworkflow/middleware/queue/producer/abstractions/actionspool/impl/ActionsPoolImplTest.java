package it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.impl;

import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypushworkflow.service.ActionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;

class ActionsPoolImplTest {

    private ActionService actionService;
    private ActionsPoolImpl actionsPool;

    @BeforeEach
    void setup() {
        actionService = Mockito.mock(ActionService.class);
        actionsPool = new ActionsPoolImpl(actionService);
    }

    @Test
    void addOnlyAction() {
        //GIVEN
        final Instant now = Instant.now();
        Action action = Action.builder()
                .iun("01")
                .actionId("001")
                .recipientIndex(0)
                .notBefore(now.minus(Duration.ofSeconds(10)))
                .type(ActionType.ANALOG_WORKFLOW)
                .build();
        //WHEN
        actionsPool.addOnlyAction(action);
        //THEN
        Mockito.verify(actionService).addOnlyActionIfAbsent(Mockito.any(Action.class));
    }

    @Test
    void unscheduleFutureAction_shouldCallUnScheduleOnService() {
        String actionId = "test-action-id";
        actionsPool.unscheduleFutureAction(actionId);
        Mockito.verify(actionService, Mockito.times(1)).unSchedule(actionId);
    }

}