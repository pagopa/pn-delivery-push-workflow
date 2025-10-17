package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypushworkflow.action.rework.ReworkHandler;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ReworkNotificationHandlerTest {

    @Mock
    private ReworkHandler reworkHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private ReworkNotificationHandler handler;

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.NOTIFICATION_REWORK_VALIDATION, handler.getSupportedEventType());
    }

    @Test
    void handleExecutesWhenNotificationNotCancelled() {
        Action action = Action.builder()
                .iun("iun_456")
                .recipientIndex(1)
                .build();

        handler.handle(action, headers);

        Mockito.verify(reworkHandler).handleRework(action);
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        Action action = Action.builder()
                .iun("iun_456")
                .recipientIndex(1)
                .build();

        Mockito.doThrow(new RuntimeException("Errore di validazione")).when(reworkHandler).handleRework(action);

        assertThrows(RuntimeException.class, () -> handler.handle(action, headers));

        Mockito.verify(reworkHandler).handleRework(action);
    }
}