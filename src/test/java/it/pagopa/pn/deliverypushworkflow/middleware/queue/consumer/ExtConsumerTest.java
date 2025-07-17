package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class ExtConsumerTest {
    @InjectMocks
    private ExtConsumer extConsumer;

    @Test
    void pnExtChannelEventInboundConsumer_processesValidMessageWithoutException() {
        Message<String> message = mock(Message.class);
        when(message.getPayload()).thenReturn("evento-valido");
        when(message.getHeaders()).thenReturn(new MessageHeaders(emptyMap()));
        extConsumer.pnExtChannelEventInboundConsumer(message);
    }

    @Test
    void pnExtChannelEventInboundConsumer_throwsExceptionWhenSetMdcFails() {
        Message<String> message = mock(Message.class);
        when(message.getPayload()).thenReturn("evento-errore");
        // Simula errore su setMdc tramite mock statico se possibile, altrimenti su getPayload
        doThrow(new RuntimeException("Errore simulato"))
                .when(message).getPayload();

        assertThrows(RuntimeException.class, () -> extConsumer.pnExtChannelEventInboundConsumer(message));
    }

    @Test
    void pnExtChannelEventInboundConsumer_throwsExceptionOnNullMessage() {
        assertThrows(NullPointerException.class, () -> extConsumer.pnExtChannelEventInboundConsumer(null));
    }
}