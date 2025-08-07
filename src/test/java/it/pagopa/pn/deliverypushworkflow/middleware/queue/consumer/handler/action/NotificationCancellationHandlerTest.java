package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypushworkflow.action.cancellation.NotificationCancellationActionHandler;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class NotificationCancellationHandlerTest {
    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private NotificationCancellationActionHandler notificationCancellationActionHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private NotificationCancellationHandler handler;

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.NOTIFICATION_CANCELLATION, handler.getSupportedEventType());
    }

    @Test
    void handleExecutesCancellation() {
        Action action = Action.builder()
                .iun("iun_123")
                .build();

        handler.handle(action, headers);

        Mockito.verify(notificationCancellationActionHandler).continueCancellationProcess("iun_123");
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        Instant notBefore = Instant.now();
        Action action = Action.builder()
                .iun("iun_123")
                .notBefore(notBefore)
                .build();

        Mockito.doThrow(new RuntimeException("Validation error")).when(notificationCancellationActionHandler).continueCancellationProcess("iun_123");

        assertThrows(RuntimeException.class, () -> handler.handle(action, headers));

        Mockito.verify(notificationCancellationActionHandler).continueCancellationProcess("iun_123");
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationCancellationRequested("iun_123");
    }
}