package it.pagopa.pn.deliverypushworkflow.middleware.responsehandler;

import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.cost.NoticePaymentInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notificationpaid.NotificationPaidInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.EventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.PublicRegistryCallDetailsInt;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.notificationcostservice.NotificationCostServiceClient;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import it.pagopa.pn.deliverypushworkflow.service.mapper.NotificationPaidMapper;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
@CustomLog
public class NoticePaymentEventHandler {

    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationService notificationService;

    public void handleResponse(NoticePaymentInt event) {
        String iun = event.getIun();
        HandleEventUtils.addIunToMdc(iun);
        String elementId = buildTimelineEventIdForNoticePaymentPaid(event);
        handleInsertNotificationPaidTimelineElement(event, elementId);
    }

    private void handleInsertNotificationPaidTimelineElement(NoticePaymentInt event, String elementId) {
        log.info("New event PAYMENT, start process to insert: {} ", event.getIuv());

        NotificationInt notification = notificationService.getNotificationByIun(event.getIun());

        TimelineElementInternal timelineElementInternal = timelineUtils.buildNoticePaymentPaidTimelineElement(notification, event, elementId);
        timelineService.addTimelineElement(timelineElementInternal, notification);
        log.info("Payment process complete: {}", event.getIuv());
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
