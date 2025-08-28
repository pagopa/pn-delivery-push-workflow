package it.pagopa.pn.deliverypushworkflow.middleware.responsehandler;

import it.pagopa.pn.deliverypushworkflow.action.analogworkflow.AnalogWorkflowDeliveryTimeoutHandler;
import it.pagopa.pn.deliverypushworkflow.action.cancellation.NotificationCancellationActionHandler;
import it.pagopa.pn.deliverypushworkflow.action.completionworkflow.AnalogFailureDeliveryCreationResponseHandler;
import it.pagopa.pn.deliverypushworkflow.action.completionworkflow.DigitalDeliveryCreationResponseHandler;
import it.pagopa.pn.deliverypushworkflow.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypushworkflow.action.notificationview.NotificationViewLegalFactCreationResponseHandler;
import it.pagopa.pn.deliverypushworkflow.action.startworkflowrecipient.AarCreationResponseHandler;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.documentcreation.DocumentCreationTypeInt;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class DocumentCreationResponseHandler {
    private final AarCreationResponseHandler aarCreationResponseHandler;
    private final NotificationViewLegalFactCreationResponseHandler notificationViewLegalFactCreationResponseHandler;
    private final DigitalDeliveryCreationResponseHandler digitalDeliveryCreationResponseHandler;
    private final AnalogFailureDeliveryCreationResponseHandler analogFailureDeliveryCreationResponseHandler;
    private final TimelineUtils timelineUtils;
    private final NotificationCancellationActionHandler notificationCancellationActionHandler;
    private final AnalogWorkflowDeliveryTimeoutHandler analogWorkflowDeliveryTimeoutHandler;

    public void handleResponseReceived( String iun, Integer recIndex, DocumentCreationResponseActionDetails details) {
        if (timelineUtils.checkIsNotificationCancellationRequested(iun) && ! DocumentCreationTypeInt.NOTIFICATION_CANCELLED.equals(details.getDocumentCreationType())){
            log.warn("DocumentCreation blocked: cancellation requested for iun {}", iun);
            return;
        }
        String fileKey = details.getKey();
        DocumentCreationTypeInt documentCreationType = details.getDocumentCreationType();

        switch (documentCreationType) {
            case AAR ->
                    aarCreationResponseHandler.handleAarCreationResponse(iun, recIndex, details);
            case ANALOG_FAILURE_DELIVERY ->
                    analogFailureDeliveryCreationResponseHandler.handleAnalogFailureDeliveryCreationResponse(iun, recIndex, details);
            case DIGITAL_DELIVERY ->
                    digitalDeliveryCreationResponseHandler.handleDigitalDeliveryCreationResponse(iun, recIndex, details);
            case RECIPIENT_ACCESS ->
                    notificationViewLegalFactCreationResponseHandler.handleLegalFactCreationResponse(iun, recIndex, details);
            case NOTIFICATION_CANCELLED ->
                    notificationCancellationActionHandler.completeCancellationProcess(iun, fileKey);
            case ANALOG_DELIVERY_TIMEOUT ->
                    analogWorkflowDeliveryTimeoutHandler.handleDeliveryTimeout(iun, recIndex, details);
        }
    }
}
