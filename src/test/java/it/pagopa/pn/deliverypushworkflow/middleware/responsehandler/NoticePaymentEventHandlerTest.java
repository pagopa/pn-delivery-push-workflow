package it.pagopa.pn.deliverypushworkflow.middleware.responsehandler;

import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.cost.NoticePaymentInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticePaymentEventHandlerTest {

    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private NoticePaymentEventHandler handler;

    @Test
    void handleResponseBuildsAndPersistsPaidTimelineElement() {
        NoticePaymentInt event = NoticePaymentInt.builder()
                .iun("iun-123")
                .recIndex(2)
                .noticeCode("notice-code")
                .creditorTaxId("tax-id")
                .build();
        NotificationInt notification = NotificationInt.builder().iun("iun-123").build();
        TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();
        when(notificationService.getNotificationByIun("iun-123")).thenReturn(notification);
        when(timelineUtils.buildNoticePaymentPaidTimelineElement(
                notification, event, "PAYMENT_NOTICE_PAID.IUN_iun-123.CODE_PPAnotice-codetax-id"))
                .thenReturn(timelineElement);

        handler.handleResponse(event);

        verify(notificationService).getNotificationByIun("iun-123");
        verify(timelineUtils).buildNoticePaymentPaidTimelineElement(
                notification, event, "PAYMENT_NOTICE_PAID.IUN_iun-123.CODE_PPAnotice-codetax-id");
        ArgumentCaptor<TimelineElementInternal> timelineElementCaptor =
                ArgumentCaptor.forClass(TimelineElementInternal.class);
        verify(timelineService).addTimelineElement(timelineElementCaptor.capture(), org.mockito.ArgumentMatchers.eq(notification));
        assertEquals(timelineElement, timelineElementCaptor.getValue());
    }

    @Test
    void buildsStableTimelineEventIdFromPaymentIdentifiers() {
        NoticePaymentInt event = NoticePaymentInt.builder()
                .iun("iun-123")
                .recIndex(2)
                .noticeCode("notice-code")
                .creditorTaxId("tax-id")
                .build();

        assertEquals("PAYMENT_NOTICE_PAID.IUN_iun-123.CODE_PPAnotice-codetax-id",
                handler.buildTimelineEventIdForNoticePaymentPaid(event));
    }
}
