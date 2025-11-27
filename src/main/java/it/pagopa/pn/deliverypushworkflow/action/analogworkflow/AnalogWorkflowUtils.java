package it.pagopa.pn.deliverypushworkflow.action.analogworkflow;


import it.pagopa.pn.deliverypushworkflow.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.externalchannel.AttachmentDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.paperchannel.SendEventInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.BaseAnalogDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.BaseRegisteredLetterDetailsInt;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@Slf4j
public class AnalogWorkflowUtils {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationUtils notificationUtils;

    public AnalogWorkflowUtils(TimelineService timelineService,
                               TimelineUtils timelineUtils,
                               NotificationUtils notificationUtils) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.notificationUtils = notificationUtils;
    }

    public String addFailureAnalogFeedbackToTimeline(NotificationInt notification, int sentAttemptMade, List<AttachmentDetailsInt> attachments,
                                                     BaseAnalogDetailsInt sendPaperDetails, SendEventInt sendEventInt, String sendRequestId) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildFailureAnalogFeedbackTimelineElement(notification, sentAttemptMade, attachments, sendPaperDetails, sendEventInt, sendRequestId);

        String insertedElementId = addTimelineElement(timelineElementInternal, notification);
        if(StringUtils.hasText(insertedElementId)){
            return insertedElementId;
        }
        return timelineElementInternal.getElementId();
    }


    public void addAnalogProgressAttemptToTimeline(NotificationInt notification, List<AttachmentDetailsInt> attachments,
                                                   BaseAnalogDetailsInt sendPaperDetails, SendEventInt sendEventInt, String sendRequestId) {
        int progressIndex = timelineService.retrieveAndIncrementCounterForTimelineEvent(sendRequestId).intValue();

        addTimelineElement(
                timelineUtils.buildAnalogProgressTimelineElement(notification, attachments, progressIndex, sendPaperDetails, sendEventInt, sendRequestId),
                notification);
    }

    public void addSimpleRegisteredLetterProgressToTimeline(NotificationInt notification, List<AttachmentDetailsInt> attachments,
                                                                   BaseRegisteredLetterDetailsInt sendPaperDetails, SendEventInt sendEventInt, String sendRequestId) {
        int progressIndex = timelineService.retrieveAndIncrementCounterForTimelineEvent(sendRequestId).intValue();

        addTimelineElement(
                timelineUtils.buildSimpleRegisteredLetterProgressTimelineElement(notification, attachments, progressIndex, sendPaperDetails, sendEventInt, sendRequestId),
                notification);
    }

    public String addSuccessAnalogFeedbackToTimeline(NotificationInt notification, List<AttachmentDetailsInt> attachments,
                                                     BaseAnalogDetailsInt sendPaperDetails, SendEventInt sendEventInt, String sendRequestId) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildSuccessAnalogFeedbackTimelineElement(
                notification,
                attachments,
                sendPaperDetails,
                sendEventInt,
                sendRequestId
        );

        String insertedElementId = addTimelineElement(timelineElementInternal, notification);
        if(StringUtils.hasText(insertedElementId)){
            return insertedElementId;
        }
        return timelineElementInternal.getElementId();
    }

    private String addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        return timelineService.addTimelineElement(element, notification).getTimelineElementId();
    }
    
    public PhysicalAddressInt getPhysicalAddress(NotificationInt notification, Integer recIndex){
        NotificationRecipientInt notificationRecipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        return notificationRecipient.getPhysicalAddress();
    }

}
