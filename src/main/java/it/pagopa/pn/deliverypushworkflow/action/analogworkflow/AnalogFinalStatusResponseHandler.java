package it.pagopa.pn.deliverypushworkflow.action.analogworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypushworkflow.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALIDEVENTCODE;
import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT;

@Slf4j
@AllArgsConstructor
@Component
public class AnalogFinalStatusResponseHandler {
    public static final String DECEASED_FAILURE_CAUSE = "M02";
    private TimelineService timelineService;
    private CompletionWorkFlowHandler completionWorkFlow;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final NotificationService notificationService;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;

    public void handleFinalResponse(String iun, int recIndex, String analogFeedbackTimelineId){
        log.debug("Start handle analog final response - iun={} id={} feedbackTimelineId={}", iun, recIndex, analogFeedbackTimelineId);
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        Optional<SendAnalogFeedbackDetailsInt> sendAnalogFeedbackDetailsOpt = timelineService.getTimelineElementDetails(iun, analogFeedbackTimelineId, SendAnalogFeedbackDetailsInt.class);
        if(sendAnalogFeedbackDetailsOpt.isPresent()){
            SendAnalogFeedbackDetailsInt sendAnalogFeedbackDetails = sendAnalogFeedbackDetailsOpt.get();
            
            switch (sendAnalogFeedbackDetails.getResponseStatus()) {
                case OK -> handleSuccessfulSending(notification, recIndex, sendAnalogFeedbackDetails);
                case KO -> handleNotSuccessfulSending(notification, recIndex, sendAnalogFeedbackDetails);
                default -> handleError(String.format("Status %s is not handled - iun=%s", sendAnalogFeedbackDetails.getResponseStatus(), iun), ERROR_CODE_DELIVERYPUSH_INVALIDEVENTCODE);
            }

        } else {
            handleError(String.format("SendAnalogFeedback %s is not present", analogFeedbackTimelineId), ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
        }
    }

    private static void handleError(String msg, String exceptionEventCode) {
        log.error(msg);
        throw new PnInternalException(msg, exceptionEventCode);
    }

    private void handleSuccessfulSending(NotificationInt notification, int recIndex, SendAnalogFeedbackDetailsInt sendAnalogFeedbackDetails) {
        EndWorkflowStatus endWorkflowStatus;
        if (DECEASED_FAILURE_CAUSE.equals(sendAnalogFeedbackDetails.getDeliveryFailureCause())
                && shouldFollowDeceasedWorkflow(notification.getSentAt())
        ) {
            endWorkflowStatus = EndWorkflowStatus.DECEASED;
        } else {
            endWorkflowStatus = EndWorkflowStatus.SUCCESS;
        }

        completionWorkFlow.completionAnalogWorkflow(
                notification,
                recIndex,
                sendAnalogFeedbackDetails.getNotificationDate(),
                sendAnalogFeedbackDetails.getPhysicalAddress(),
                endWorkflowStatus
        );
    }
    
    private void handleNotSuccessfulSending(NotificationInt notification, int recIndex, SendAnalogFeedbackDetailsInt sendAnalogFeedbackDetails) {
        int sentAttemptMade = sendAnalogFeedbackDetails.getSentAttemptMade() + 1;
        analogWorkflowHandler.nextWorkflowStep(notification, recIndex, sentAttemptMade, sendAnalogFeedbackDetails.getNotificationDate());
    }

    private boolean shouldFollowDeceasedWorkflow(Instant notificationDate) {
        String activationDate = pnDeliveryPushWorkflowConfigs.getActivationDeceasedWorkflowDate();
        return activationDate != null && notificationDate.isAfter(Instant.parse(activationDate));
    }

}
