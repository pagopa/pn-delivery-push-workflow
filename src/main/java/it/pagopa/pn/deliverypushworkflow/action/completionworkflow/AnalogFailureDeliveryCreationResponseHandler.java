package it.pagopa.pn.deliverypushworkflow.action.completionworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypushworkflow.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.CompletelyUnreachableCreationRequestDetails;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND;

@Component
@AllArgsConstructor
@CustomLog
public class AnalogFailureDeliveryCreationResponseHandler {
    private final CompletelyUnreachableUtils completelyUnreachableUtils;
    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final RefinementScheduler refinementScheduler;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;

    public void handleAnalogFailureDeliveryCreationResponse(String iun, int recIndex, DocumentCreationResponseActionDetails actionDetails) {
        log.info("Start handleAnalogFailureDeliveryCreationResponse - iun={} recIndex={} aarKey={}", iun, recIndex, actionDetails.getKey());
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        Instant featureUnreachableRefinementPostAARStartDate = pnDeliveryPushWorkflowConfigs.getFeatureUnreachableRefinementPostAARStartDate();

        PnAuditLogEvent logEvent = createAuditLog(notification, recIndex, actionDetails.getKey());
        logEvent.log();
        
        Optional<CompletelyUnreachableCreationRequestDetails> completelyUnreachableCreationRequestDetailsOpt = timelineService.getTimelineElementDetails(iun, actionDetails.getTimelineId(), CompletelyUnreachableCreationRequestDetails.class);

        if (completelyUnreachableCreationRequestDetailsOpt.isPresent()) {
            CompletelyUnreachableCreationRequestDetails completelyUnreachableCreationRequestDetails = completelyUnreachableCreationRequestDetailsOpt.get();
            // recupero la data di generazione del DEPOSITO AAR, per poterla inserire nell'atto opponibile
            TimelineElementInternal analogFailureWorkflowTimelineElement = retrieveAnalogFailureWorkflowTimelineElement(notification.getIun(), recIndex);
        
            Instant scheduleRefinementDate = completelyUnreachableCreationRequestDetails.getCompletionWorkflowDate(); //Data business evento di feedback irreperibile, data che viene ancora utilizzata per notifiche precedenti alla fix ma risulta una data errata
            if(notification.getSentAt() != null && notification.getSentAt().isAfter(featureUnreachableRefinementPostAARStartDate)) {
                scheduleRefinementDate = analogFailureWorkflowTimelineElement.getTimestamp(); //data corretta da utilizzare per tutte le notifiche generate a valle della fix
            }

            completelyUnreachableUtils.handleCompletelyUnreachable(notification, recIndex, completelyUnreachableCreationRequestDetails.getLegalFactId(), analogFailureWorkflowTimelineElement.getTimestamp());
            refinementScheduler.scheduleAnalogRefinement(notification, recIndex, scheduleRefinementDate, completelyUnreachableCreationRequestDetails.getEndWorkflowStatus());

        } else {
            logEvent.generateFailure("Error in handleAnalogFailureDeliveryCreationResponse for timelineId={} and key={} - iun={} recIndex={}", actionDetails.getTimelineId(), actionDetails.getKey(), notification.getIun(), recIndex).log();
        }
    }

    private PnAuditLogEvent createAuditLog(NotificationInt notification, int recIndex, String legalFactId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        return auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_NEWLEGAL, "Saving legalFact type={} fileKey={} - iun={} recIndex={}", LegalFactCategoryInt.ANALOG_FAILURE_DELIVERY, legalFactId, notification.getIun(), recIndex)
                .iun(notification.getIun())
                .build();
    }


    private TimelineElementInternal retrieveAnalogFailureWorkflowTimelineElement(String iun, int recIndex) {
        log.info("retrieveAARUrlFromTimeline iun={} recIndex={}", iun, recIndex);

        Optional<TimelineElementInternal> timelineEvent = timelineService.getTimelineElementForSpecificRecipient(iun, recIndex, TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW);

        if (timelineEvent.isPresent()) {
            return timelineEvent.get();
        }
        else
        {
            log.fatal("Cannot retrieve AnalogFailureWorkflow for iun={} recIndex={}", iun, recIndex);
            throw new PnInternalException("Cannot retrieve AnalogFailureWorkflow for Iun " + iun + " id" + recIndex, ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND);
        }
    }
}
