package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
class ActionConsumerTest {
    @InjectMocks
    private ActionConsumer actionConsumer;

    @Test
    void pnDeliveryPushActionsInboundConsumer_logsMessageOnValidInput() {
        Message<String> message = Mockito.mock(Message.class);
        Mockito.when(message.getPayload()).thenReturn("valid-payload");
        Mockito.when(message.getHeaders()).thenReturn(new MessageHeaders(emptyMap()));

        actionConsumer.pnDeliveryPushActionsInboundConsumer(message);
    }

    @Test
    void pnDeliveryPushActionsInboundConsumer_throwsExceptionOnProcessingError() {
        Message<String> message = Mockito.mock(Message.class);
        Mockito.when(message.getPayload()).thenReturn("error-payload");

        // Simula un errore nel setMdc tramite un mock statico se necessario
        // oppure simula un errore nel message.getPayload()
        Mockito.doThrow(new RuntimeException("Simulated error"))
                .when(message).getPayload();

        assertThrows(RuntimeException.class, () -> actionConsumer.pnDeliveryPushActionsInboundConsumer(message));
    }

    @Test
    void pnDeliveryPushActionsInboundConsumer_handlesNullMessageGracefully() {
        assertThrows(NullPointerException.class, () -> actionConsumer.pnDeliveryPushActionsInboundConsumer(null));
    }

}