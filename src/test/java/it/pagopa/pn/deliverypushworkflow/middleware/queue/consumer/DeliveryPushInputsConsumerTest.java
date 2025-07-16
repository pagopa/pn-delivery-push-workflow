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
class DeliveryPushInputsConsumerTest {
    @InjectMocks
    private DeliveryPushInputsConsumer deliveryPushInputsConsumer;

    @Test
    void pnDeliveryPushInputsInboundConsumer_logsMessageOnValidInput() {
        Message<String> message = mock(Message.class);
        when(message.getPayload()).thenReturn("notifica-valida");
        when(message.getHeaders()).thenReturn(new MessageHeaders(emptyMap()));

        deliveryPushInputsConsumer.pnDeliveryPushInputsInboundConsumer(message);
    }

    @Test
    void pnDeliveryPushInputsInboundConsumer_throwsExceptionOnProcessingError() {
        Message<String> message = mock(Message.class);
        when(message.getPayload()).thenReturn("errore-notifica");
        doThrow(new RuntimeException("Errore simulato"))
                .when(message).getPayload();

        assertThrows(RuntimeException.class, () -> deliveryPushInputsConsumer.pnDeliveryPushInputsInboundConsumer(message));
    }

    @Test
    void pnDeliveryPushInputsInboundConsumer_handlesNullMessageGracefully() {
        assertThrows(NullPointerException.class, () -> deliveryPushInputsConsumer.pnDeliveryPushInputsInboundConsumer(null));
    }
}