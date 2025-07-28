package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.nationalregistries.model.AddressSQSMessage;
import it.pagopa.pn.deliverypushworkflow.middleware.responsehandler.NationalRegistriesResponseHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class NationalRegistriesConsumerTest {
    @Mock
    NationalRegistriesResponseHandler nationalRegistriesResponseHandler;

    @InjectMocks
    private NationalRegistriesConsumer nationalRegistriesConsumer;

    @Test
    void pnNationalRegistriesEventInboundConsumer_processesValidMessageWithoutException() {
        Message<AddressSQSMessage> message = mock(Message.class);
        when(message.getPayload()).thenReturn(new AddressSQSMessage());
        when(message.getHeaders()).thenReturn(new MessageHeaders(emptyMap()));

        nationalRegistriesConsumer.pnNationalRegistriesEventInboundConsumer(message);

        verify(nationalRegistriesResponseHandler, times(1)).handleResponse(any());
    }

    @Test
    void pnNationalRegistriesEventInboundConsumer_throwsExceptionOnProcessingError() {
        Message<AddressSQSMessage> message = mock(org.springframework.messaging.Message.class);
        doThrow(new RuntimeException("Errore simulato"))
                .when(message).getPayload();

        assertThrows(RuntimeException.class, () -> nationalRegistriesConsumer.pnNationalRegistriesEventInboundConsumer(message));

        verify(nationalRegistriesResponseHandler, times(0)).handleResponse(any());
    }

    @Test
    void pnNationalRegistriesEventInboundConsumer_throwsExceptionOnNullMessage() {
        assertThrows(NullPointerException.class, () -> nationalRegistriesConsumer.pnNationalRegistriesEventInboundConsumer(null));
    }
}
