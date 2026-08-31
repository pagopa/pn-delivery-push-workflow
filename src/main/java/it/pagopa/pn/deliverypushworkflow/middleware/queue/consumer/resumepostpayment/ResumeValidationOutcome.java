package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.resumepostpayment;

public enum ResumeValidationOutcome {
    ELIGIBLE,
    ALREADY_PROCESSED,
    NOT_ELIGIBLE
}