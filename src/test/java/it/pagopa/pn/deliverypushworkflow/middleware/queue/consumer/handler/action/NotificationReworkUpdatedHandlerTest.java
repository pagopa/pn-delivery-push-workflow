package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypushworkflow.action.rework.ReworkUpdateHandler;
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
class NotificationReworkUpdatedHandlerTest {

    @Mock
    private ReworkUpdateHandler reworkUpdateHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private NotificationReworkUpdatedHandler handler;

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.NOTIFICATION_REWORK_UPDATE, handler.getSupportedEventType());
    }

    @Test
    void handleExecutesWhenNotificationNotCancelled() {
        Action action = Action.builder()
                .iun("iun_789")
                .recipientIndex(2)
                .build();

        when(reworkUpdateHandler.handleNotificationReworkUpdate(any())).thenReturn(Mono.empty());

        handler.handle(action, headers);

        Mockito.verify(reworkUpdateHandler).handleNotificationReworkUpdate(action);
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        Action action = Action.builder()
                .iun("iun_789")
                .recipientIndex(2)
                .build();

        Mockito.doThrow(new RuntimeException("Errore di update")).when(reworkUpdateHandler).handleNotificationReworkUpdate(action);

        assertThrows(RuntimeException.class, () -> handler.handle(action, headers));

        Mockito.verify(reworkUpdateHandler).handleNotificationReworkUpdate(action);
    }
}

