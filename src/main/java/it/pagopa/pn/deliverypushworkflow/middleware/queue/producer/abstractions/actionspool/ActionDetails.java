package it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.pagopa.pn.deliverypushworkflow.action.details.*;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "actionType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NotHandledDetails.class, name = "ANALOG_WORKFLOW"),
        @JsonSubTypes.Type(value = RecipientsWorkflowDetails.class, name = "START_RECIPIENT_WORKFLOW"),
        @JsonSubTypes.Type(value = NotHandledDetails.class, name = "CHOOSE_DELIVERY_MODE"),
        @JsonSubTypes.Type(value = NotHandledDetails.class, name = "ANALOG_WORKFLOW"),
        @JsonSubTypes.Type(value = NotHandledDetails.class, name = "DIGITAL_WORKFLOW_NEXT_ACTION"),
        @JsonSubTypes.Type(value = NotHandledDetails.class, name = "DIGITAL_WORKFLOW_NEXT_EXECUTE_ACTION"),
        @JsonSubTypes.Type(value = NotHandledDetails.class, name = "DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION"),
        @JsonSubTypes.Type(value = NotHandledDetails.class, name = "DIGITAL_WORKFLOW_RETRY_ACTION"),
        @JsonSubTypes.Type(value = NotHandledDetails.class, name = "REFINEMENT_NOTIFICATION"),
        @JsonSubTypes.Type(value = NotHandledDetails.class, name = "SENDER_ACK"),
        @JsonSubTypes.Type(value = DocumentCreationResponseActionDetails.class, name = "DOCUMENT_CREATION_RESPONSE"),
        @JsonSubTypes.Type(value = SendDigitalFinalStatusResponseDetails.class, name = "SEND_DIGITAL_FINAL_STATUS_RESPONSE"),
        @JsonSubTypes.Type(value = AnalogWorkflowTimeoutDetails.class, name = "ANALOG_WORKFLOW_NO_FEEDBACK_TIMEOUT"),
        @JsonSubTypes.Type(value = NotificationReworkValidationDetails.class, name = "NOTIFICATION_REWORK_VALIDATION"),
        @JsonSubTypes.Type(value = NotificationReworkRequestedDetails.class, name = "NOTIFICATION_REWORK_REQUESTED"),
        @JsonSubTypes.Type(value = NotificationReworkUpdateDetails.class, name = "NOTIFICATION_REWORK_UPDATE"),
})
public interface ActionDetails {

}
