package it.pagopa.pn.deliverypushworkflow.action.analogworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypushworkflow.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypushworkflow.action.utils.AnalogDeliveryTimeoutUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogTimeoutCreationRequestDetailsInt;
import it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@AllArgsConstructor
@Slf4j
@CustomLog
public class AnalogWorkflowDeliveryTimeoutHandler {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationService notificationService;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final AnalogDeliveryTimeoutUtils analogDeliveryTimeoutUtils;

    public void handleDeliveryTimeout(String iun, int recIndex, DocumentCreationResponseActionDetails actionDetails) {
        log.info("Start handleDeliveryTimeout process - iun={} recIndex={} legalFactId={}", iun, recIndex, actionDetails.getKey());
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        PnAuditLogEvent auditLogEvent = generateAuditLog(iun, recIndex, actionDetails.getKey());
        try {
            Optional<SendAnalogTimeoutCreationRequestDetailsInt> sendAnalogTimeoutCreationRequestDetailsOpt = timelineService.getTimelineElementDetails(iun, actionDetails.getTimelineId(), SendAnalogTimeoutCreationRequestDetailsInt.class);
            if (sendAnalogTimeoutCreationRequestDetailsOpt.isPresent()) {
                SendAnalogTimeoutCreationRequestDetailsInt timelineDetails = sendAnalogTimeoutCreationRequestDetailsOpt.get();
                switch (timelineDetails.getSentAttemptMade()) {
                    case 0:
                        handleFirstAttempt(timelineDetails, notification, recIndex, auditLogEvent);
                        break;
                    case 1:
                        handleSecondAttempt(timelineDetails, notification, recIndex, auditLogEvent);
                }
            }
        } catch (Exception ex) {
            throw new PnInternalException("Unexpected error: " + ex.getMessage(), PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINENOTFOUND);
        }
    }

    private void handleFirstAttempt(SendAnalogTimeoutCreationRequestDetailsInt sendAnalogTimeoutCreationRequestDetailsInt,
                                    NotificationInt notification,
                                    int recIndex,
                                    PnAuditLogEvent auditLogEvent){
        String iun = notification.getIun();
        log.info("First sent attempt - iun={} id={}", iun, recIndex);
        int sentAttemptMade = sendAnalogTimeoutCreationRequestDetailsInt.getSentAttemptMade();
        buildSendAnalogTimeoutElement(sendAnalogTimeoutCreationRequestDetailsInt, notification, recIndex, auditLogEvent);

        if (timelineUtils.checkIsNotificationViewed(iun, recIndex)) {
            log.info("Notification with iun={} viewed by recipient with index={}, second attempt will not be scheduled", iun, recIndex);
        } else {
            log.info("Notification with iun={} not viewed by recipient with index={}, second attempt will be scheduled", iun, recIndex);
            int sentAttemptMadeForSecondAttempt = sentAttemptMade + 1;
            analogWorkflowHandler.nextWorkflowStep(notification, recIndex, sentAttemptMadeForSecondAttempt, null);
        }
    }

    private void buildSendAnalogTimeoutElement(SendAnalogTimeoutCreationRequestDetailsInt sendAnalogTimeoutCreationRequestDetailsInt,
                                               NotificationInt notification,
                                               int recIndex,
                                               PnAuditLogEvent auditLogEvent){
        String iun = notification.getIun();
        /*
        Recupero l'elemento di timeline con category SEND_ANALOG_DOMICILE legato all'elemento di timeline con category SEND_ANALOG_TIMEOUT_CREATION_REQUEST
        Poichè nel dettaglio contiene una serie di informazioni relative all'affido dell'invio analogico, che devono essere riportate nell'elemento di timeline SEND_ANALOG_TIMEOUT
         */
        String sendAnalogDomicileTimelineId = sendAnalogTimeoutCreationRequestDetailsInt.getRelatedRequestId();
        Optional<SendAnalogDetailsInt> sendAnalogDetailsOpt =
                timelineService.getTimelineElementDetails(iun, sendAnalogDomicileTimelineId, SendAnalogDetailsInt.class);
        if (sendAnalogDetailsOpt.isPresent()) {
            SendAnalogDetailsInt sendAnalogDetails = sendAnalogDetailsOpt.get();
            String legalFactId = sendAnalogTimeoutCreationRequestDetailsInt.getLegalFactId();
            Instant timeoutDate = sendAnalogTimeoutCreationRequestDetailsInt.getTimeoutDate();
            TimelineElementInternal sendAnalogTimeoutElementInternal = timelineUtils.buildSendAnalogTimeout(notification, sendAnalogDetails, timeoutDate, legalFactId, sendAnalogDomicileTimelineId);
            timelineService.addTimelineElement(sendAnalogTimeoutElementInternal, notification);
            auditLogEvent.generateSuccess("SEND_ANALOG_TIMEOUT successfully added for recIndex={} and sentAttempt={}", recIndex, sendAnalogDetails.getSentAttemptMade()).log();
        } else {
            log.error("SendAnalogDetails not found for iun={} and timelineId={}", iun, sendAnalogTimeoutCreationRequestDetailsInt.getRelatedRequestId());
        }
    }

    private void handleSecondAttempt(SendAnalogTimeoutCreationRequestDetailsInt sendAnalogTimeoutCreationRequestDetailsInt,
                                     NotificationInt notification,
                                     int recIndex,
                                     PnAuditLogEvent auditLogEvent){
        log.info("Second sent attempt - iun={} id={}", notification.getIun(), recIndex);
        try {
            Instant timeoutDate = sendAnalogTimeoutCreationRequestDetailsInt.getTimeoutDate();
            buildSendAnalogTimeoutElement(sendAnalogTimeoutCreationRequestDetailsInt, notification, recIndex, auditLogEvent);
            analogDeliveryTimeoutUtils.buildAnalogFailureWorkflowTimeoutElement(notification, recIndex, timeoutDate);
            auditLogEvent.generateSuccess("ANALOG_FAILURE_WORKFLOW_TIMEOUT successfully added for recIndex={}", recIndex).log();
        } catch (Exception exc) {
            auditLogEvent.generateFailure("Unexpected error handling second attempt", exc).log();
            throw exc;
        }
    }

    @NotNull
    private PnAuditLogEvent generateAuditLog(String iun, int recIndex, String legalFactId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        return auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_ANALOG_TIMEOUT, "Saving legalFact type={} fileKey={} - iun={} recIndex={}", LegalFactCategoryInt.ANALOG_DELIVERY_TIMEOUT, legalFactId, iun, recIndex)
                .iun(iun)
                .build();
    }
}
