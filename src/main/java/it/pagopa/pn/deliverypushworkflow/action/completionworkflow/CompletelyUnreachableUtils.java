package it.pagopa.pn.deliverypushworkflow.action.completionworkflow;

import it.pagopa.pn.deliverypushworkflow.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class CompletelyUnreachableUtils {
    private final PaperNotificationFailedService paperNotificationFailedService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationUtils notificationUtils;

    public CompletelyUnreachableUtils(PaperNotificationFailedService paperNotificationFailedService,
                                      TimelineService timelineService,
                                      TimelineUtils timelineUtils,
                                      NotificationUtils notificationUtils) {
        this.paperNotificationFailedService = paperNotificationFailedService;
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.notificationUtils = notificationUtils;
    }

    public void handleCompletelyUnreachable(NotificationInt notification, Integer recIndex, String legalFactId, Instant legalFactGenerationDate) {
        log.info("HandleCompletelyUnreachable - iun {} id {} ", notification.getIun(), recIndex);
        boolean isNotificationViewed = timelineUtils.checkIsNotificationViewed(notification.getIun(), recIndex);

        // solo nel caso di notifica visualizzata, non serve inserire il record di paper notification failed
        if (!isNotificationViewed) {
            addPaperNotificationFailed(notification, recIndex);
        }
        addTimelineElement( 
                timelineUtils.buildCompletelyUnreachableTimelineElement(notification, recIndex, legalFactId, legalFactGenerationDate),
                notification);
    }

    private void addPaperNotificationFailed(NotificationInt notification, Integer recIndex) {
        log.info("AddPaperNotificationFailed - iun {} id {} ", notification.getIun(), recIndex);
        
        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);
        
        paperNotificationFailedService.addPaperNotificationFailed(
                PaperNotificationFailed.builder()
                        .iun(notification.getIun())
                        .recipientId(recipient.getInternalId())
                        .build()
        );
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

}
