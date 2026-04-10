package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.model.PaperChannelUpdate;
import it.pagopa.pn.deliverypushworkflow.middleware.responsehandler.PaperChannelResponseHandler;
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
class AnalogResponseEventConsumerTest {
    @Mock
    PaperChannelResponseHandler paperChannelResponseHandler;

    @InjectMocks
    private AnalogResponseConsumer analogResponseConsumer;

    @Test
    void analogResponseEventConsumer_processesValidMessageWithoutException() {
        Message<PaperChannelUpdate> message = mock(Message.class);
        when(message.getPayload()).thenReturn(new PaperChannelUpdate());
        when(message.getHeaders()).thenReturn(new MessageHeaders(emptyMap()));

        analogResponseConsumer.analogResponseEventConsumer(message);

        verify(paperChannelResponseHandler, times(1)).paperChannelResponseReceiver(any());
    }

    @Test
    void analogResponseEventConsumer_throwsExceptionOnProcessingError() {
        Message<PaperChannelUpdate> message = mock(org.springframework.messaging.Message.class);
        doThrow(new RuntimeException("Errore simulato"))
                .when(message).getPayload();

        assertThrows(RuntimeException.class, () -> analogResponseConsumer.analogResponseEventConsumer(message));

        verify(paperChannelResponseHandler, times(0)).paperChannelResponseReceiver(any());
    }

    @Test
    void analogResponseEventConsumer_throwsExceptionOnNullMessage() {
        assertThrows(NullPointerException.class, () -> analogResponseConsumer.analogResponseEventConsumer(null));
    }
}
