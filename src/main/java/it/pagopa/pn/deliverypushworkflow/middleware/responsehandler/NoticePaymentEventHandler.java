package it.pagopa.pn.deliverypushworkflow.middleware.responsehandler;

import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.cost.NoticePaymentInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.EventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@CustomLog
public class NoticePaymentEventHandler {

    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationService notificationService;

    public void handleResponse(NoticePaymentInt event) {
        String iun = event.getIun();
        log.info("New event PAYMENT, start process to insert on notification: {} for recIndex: {}, noticeCode: {} and creditorTaxId: {}", iun, event.getRecIndex(), event.getNoticeCode(), event.getCreditorTaxId());

        NotificationInt notification = notificationService.getNotificationByIun(event.getIun());
        String elementId = buildTimelineEventIdForNoticePaymentPaid(event);

        TimelineElementInternal timelineElementInternal = timelineUtils.buildNoticePaymentPaidTimelineElement(notification, event, elementId);
        timelineService.addTimelineElement(timelineElementInternal, notification);
        log.info("Payment process completed for notification: {} and recIndex: {}", iun, event.getRecIndex());
    }

    public String buildTimelineEventIdForNoticePaymentPaid(NoticePaymentInt element) {
        return TimelineEventId.PAYMENT_NOTICE_PAID.buildEventId(
                EventId.builder()
                        .iun(element.getIun())
                        .recIndex(element.getRecIndex())
                        .creditorTaxId(element.getCreditorTaxId())
                        .noticeCode(element.getNoticeCode())
                        .build());
    }
}
