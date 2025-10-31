package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypushworkflow.action.rework.ReworkRequestedHandler;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class NotificationReworkRequestedHandlerTest {

    @Mock
    private ReworkRequestedHandler reworkRequestedHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private NotificationReworkRequestedHandler handler;

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.NOTIFICATION_REWORK_REQUESTED, handler.getSupportedEventType());
    }

    @Test
    void handleExecutesWhenNotificationNotCancelled() {
        Action action = Action.builder()
                .iun("iun_456")
                .recipientIndex(1)
                .build();

        when(reworkRequestedHandler.handleNotificationReworkRequested(any())).thenReturn(Mono.empty());

        handler.handle(action, headers);

        Mockito.verify(reworkRequestedHandler).handleNotificationReworkRequested(action);
    }

}