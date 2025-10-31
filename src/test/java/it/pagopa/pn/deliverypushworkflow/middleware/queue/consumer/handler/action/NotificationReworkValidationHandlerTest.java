package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypushworkflow.action.rework.ReworkValidationHandler;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationReworkValidationHandlerTest {

    @Mock
    private ReworkValidationHandler notificationReworkHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private NotificationReworkValidationHandler handler;

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

        when(notificationReworkHandler.handleNotificationRework(any())).thenReturn(Mono.empty());

        handler.handle(action, headers);

        Mockito.verify(notificationReworkHandler).handleNotificationRework(action);
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        Action action = Action.builder()
                .iun("iun_456")
                .recipientIndex(1)
                .build();

        Mockito.doThrow(new RuntimeException("Errore di validazione")).when(notificationReworkHandler).handleNotificationRework(action);

        assertThrows(RuntimeException.class, () -> handler.handle(action, headers));

        Mockito.verify(notificationReworkHandler).handleNotificationRework(action);
    }
}