package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer;

import it.pagopa.pn.api.dto.events.PnNoticePaymentPaidEvent;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.cost.NoticePaymentInt;
import it.pagopa.pn.deliverypushworkflow.middleware.responsehandler.NoticePaymentEventHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticePaymentEventConsumerTest {

    @Mock
    private PnDeliveryPushWorkflowConfigs configs;
    @Mock
    private NoticePaymentEventHandler noticePaymentEventHandler;
    @InjectMocks
    private NoticePaymentEventConsumer consumer;

    @Test
    void consumesAndMapsNoticePaymentPaidEvent() {
        PnNoticePaymentPaidEvent event = noticePaymentEvent("iun-123", 1);
        Message<PnNoticePaymentPaidEvent> message = MessageBuilder.withPayload(event)
                .setHeader("aws_messageId", "message-id")
                .build();

        consumer.noticePaymentEventConsumer(message);

        ArgumentCaptor<NoticePaymentInt> eventCaptor = ArgumentCaptor.forClass(NoticePaymentInt.class);
        verify(noticePaymentEventHandler).handleResponse(eventCaptor.capture());
        assertEquals("iun-123", eventCaptor.getValue().getIun());
        assertEquals(1, eventCaptor.getValue().getRecIndex());
    }

    @Test
    void propagatesHandlerFailure() {
        PnNoticePaymentPaidEvent event = noticePaymentEvent("iun-123", 1);
        Message<PnNoticePaymentPaidEvent> message = MessageBuilder.withPayload(event).build();
        RuntimeException failure = new RuntimeException("processing failed");
        doThrow(failure).when(noticePaymentEventHandler).handleResponse(any(NoticePaymentInt.class));

        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> consumer.noticePaymentEventConsumer(message));

        assertEquals(failure, actual);
        verify(noticePaymentEventHandler).handleResponse(any(NoticePaymentInt.class));
    }

    private PnNoticePaymentPaidEvent noticePaymentEvent(String iun, int recIndex) {
        PnNoticePaymentPaidEvent event = org.mockito.Mockito.mock(PnNoticePaymentPaidEvent.class);
        PnNoticePaymentPaidEvent.Payload payload = org.mockito.Mockito.mock(PnNoticePaymentPaidEvent.Payload.class);
        when(event.getPayload()).thenReturn(payload);
        when(payload.getIun()).thenReturn(iun);
        when(payload.getRecIndex()).thenReturn(recIndex);
        return event;
    }
}
