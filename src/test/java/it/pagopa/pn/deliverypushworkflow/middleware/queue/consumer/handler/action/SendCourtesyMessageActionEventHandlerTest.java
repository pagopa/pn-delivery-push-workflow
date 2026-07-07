package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypushworkflow.action.details.SendCourtesyMessageActionDetails;
import it.pagopa.pn.deliverypushworkflow.action.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.MessageHeaders;

import static org.mockito.Mockito.*;

class SendCourtesyMessageActionEventHandlerTest {
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private CourtesyMessageUtils courtesyMessageUtils;
    @Mock
    private MessageHeaders headers;

    private SendCourtesyMessageActionEventHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new SendCourtesyMessageActionEventHandler(timelineUtils, courtesyMessageUtils);
    }

    @Test
    void handleCallsCourtesyMessageUtils() {
        String iun = "iun";
        int recIndex = 1;
        SendCourtesyMessageActionDetails details = SendCourtesyMessageActionDetails.builder()
                .channel(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)
                .retryIndex(0)
                .deliveryMode(DeliveryModeInt.ANALOG)
                .build();

        Action action = mock(Action.class);
        when(action.getIun()).thenReturn(iun);
        when(action.getRecipientIndex()).thenReturn(recIndex);
        when(action.getDetails()).thenReturn(details);

        handler.handle(action, headers);

        verify(courtesyMessageUtils, times(1)).handleSendCourtesyMessageAction(iun, recIndex, details);
    }

    @Test
    void handleSkipsWhenNotificationCancelled() {
        String iun = "iun";
        Action action = mock(Action.class);
        when(action.getIun()).thenReturn(iun);
        when(action.getRecipientIndex()).thenReturn(1);
        when(timelineUtils.checkIsNotificationCancellationRequested(iun)).thenReturn(true);

        handler.handle(action, headers);

        verify(courtesyMessageUtils, never()).handleSendCourtesyMessageAction(any(), any(), any());
    }

    @Test
    void getSupportedEventType() {
        Assertions.assertEquals(SupportedEventType.SEND_COURTESY_MESSAGE_ACTION, handler.getSupportedEventType());
    }
}
