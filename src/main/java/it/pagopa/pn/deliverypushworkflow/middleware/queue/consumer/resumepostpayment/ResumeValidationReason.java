package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.resumepostpayment;

public enum ResumeValidationReason {
    VALID("Restart validation completed successfully"),
    RECIPIENT_NOT_FOUND("Recipient index does not identify an existing recipient"),
    NOTIFICATION_IUN_MISMATCH("Loaded notification IUN does not match the requested IUN"),
    PAYMENT_NOT_FOUND("Payment timeline element not found for recipient"),
    NOTIFICATION_CANCELLED("Notification cancellation was requested"),
    NOTIFICATION_VIEWED("Notification was already viewed by recipient"),
    NOTIFICATION_REWORKED("Notification timeline was reworked for recipient"),
    PREPARE_ALREADY_PRESENT("Expected prepare timeline element is already present"),
    SCHEDULE_ANALOG_WORKFLOW_NOT_FOUND("Analog workflow schedule was not found"),
    SCHEDULE_ANALOG_WORKFLOW_TOO_RECENT("Analog workflow schedule is less than ten hours old"),
    FIRST_ANALOG_SEND_NOT_FOUND("First analog send was not found"),
    SECOND_ATTEMPT_TRIGGER_NOT_FOUND("Neither KO feedback nor analog timeout was found"),
    DIGITAL_FAILURE_WORKFLOW_NOT_FOUND("Digital failure workflow was not found"),
    SCHEDULE_REFINEMENT_NOT_FOUND("Refinement schedule was not found"),
    PHYSICAL_ADDRESS_NOT_FOUND("Recipient physical address was not found");

    private final String description;

    ResumeValidationReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}