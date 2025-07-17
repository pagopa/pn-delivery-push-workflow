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
class NationalRegistriesConsumerTest {
    @InjectMocks
    private NationalRegistriesConsumer nationalRegistriesConsumer;

    @Test
    void pnNationalRegistriesEventInboundConsumer_processesValidMessageWithoutException() {
        Message<String> message = mock(Message.class);
        when(message.getPayload()).thenReturn("evento-nazionale-valido");
        when(message.getHeaders()).thenReturn(new MessageHeaders(emptyMap()));

        nationalRegistriesConsumer.pnNationalRegistriesEventInboundConsumer(message);
    }

    @Test
    void pnNationalRegistriesEventInboundConsumer_throwsExceptionOnProcessingError() {
        Message<String> message = mock(org.springframework.messaging.Message.class);
        when(message.getPayload()).thenReturn("evento-errore");
        doThrow(new RuntimeException("Errore simulato"))
                .when(message).getPayload();

        assertThrows(RuntimeException.class, () -> nationalRegistriesConsumer.pnNationalRegistriesEventInboundConsumer(message));
    }

    @Test
    void pnNationalRegistriesEventInboundConsumer_throwsExceptionOnNullMessage() {
        assertThrows(NullPointerException.class, () -> nationalRegistriesConsumer.pnNationalRegistriesEventInboundConsumer(null));
    }
}
