package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.resumepostpayment;

import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;

public record ResumeValidationResult(
        ResumeValidationOutcome outcome,
        ResumeValidationReason reason,
        NotificationInt notification
) {
    public static ResumeValidationResult eligible(NotificationInt notification) {
        return new ResumeValidationResult(ResumeValidationOutcome.ELIGIBLE, ResumeValidationReason.VALID, notification);
    }

    public static ResumeValidationResult alreadyProcessed(NotificationInt notification) {
        return new ResumeValidationResult(ResumeValidationOutcome.ALREADY_PROCESSED,
                ResumeValidationReason.PREPARE_ALREADY_PRESENT, notification);
    }

    public static ResumeValidationResult notEligible(ResumeValidationReason reason, NotificationInt notification) {
        return new ResumeValidationResult(ResumeValidationOutcome.NOT_ELIGIBLE, reason, notification);
    }
}