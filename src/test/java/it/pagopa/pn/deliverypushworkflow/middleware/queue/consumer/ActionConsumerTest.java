package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer;

import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.router.EventRouter;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
class ActionConsumerTest {
    @InjectMocks
    private ActionConsumer actionConsumer;
    @Mock
    private EventRouter eventRouter;

    @Test
    void pnDeliveryPushActionsInboundConsumer_routesMessageSuccessfully() {
        Action action = Action.builder().iun("test_IUN").recipientIndex(0).type(ActionType.ANALOG_WORKFLOW).build();
        Message<Action> message = Mockito.mock(Message.class);
        Mockito.when(message.getPayload()).thenReturn(action);
        Mockito.when(message.getHeaders()).thenReturn(new MessageHeaders(Map.of("test", "headerValue")));

        actionConsumer.pnDeliveryPushActionsInboundConsumer(message);

        EventRouter.RoutingConfig expectedConfig = EventRouter.RoutingConfig.builder()
                .eventType(ActionType.ANALOG_WORKFLOW.name())
                .build();
        Mockito.verify(eventRouter).route(message, expectedConfig);
    }

    @Test
    void pnDeliveryPushActionsInboundConsumer_handlesExceptionGracefully() {
        Action action = Action.builder().iun("test_IUN").recipientIndex(0).build();
        Message<Action> message = Mockito.mock(Message.class);
        Mockito.when(message.getPayload()).thenReturn(action);
        Mockito.when(message.getHeaders()).thenReturn(new MessageHeaders(Map.of("test", "headerValue")));

        assertThrows(RuntimeException.class, () -> actionConsumer.pnDeliveryPushActionsInboundConsumer(message));

        Mockito.verify(eventRouter, Mockito.never()).route(Mockito.any(), Mockito.any());
    }


    @NotNull
    private static Message<Action> getActionMessage() {
        return new Message<>() {
            @Override
            @NotNull
            public Action getPayload() {
                return Action.builder()
                        .iun("test_IUN")
                        .recipientIndex(0)
                        .timelineId("testTimelineId")
                        .notBefore(Instant.EPOCH)
                        .build();
            }

            @Override
            @NotNull
            public MessageHeaders getHeaders() {
                return new MessageHeaders(new HashMap<>());
            }
        };
    }

}