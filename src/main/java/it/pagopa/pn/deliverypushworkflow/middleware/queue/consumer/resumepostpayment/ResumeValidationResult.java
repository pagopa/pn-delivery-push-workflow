package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.resumepostpayment;

import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;

import java.util.Collections;
import java.util.Set;

public record ResumeValidationResult(
        ResumeValidationOutcome outcome,
        ResumeValidationReason reason,
        NotificationInt notification,
        Set<TimelineElementCategoryInt> relevantTimelineCategories
) {
    public static ResumeValidationResult eligible(NotificationInt notification) {
        return new ResumeValidationResult(ResumeValidationOutcome.ELIGIBLE, ResumeValidationReason.VALID,
                notification, Collections.emptySet());
    }

    public static ResumeValidationResult alreadyProcessed(NotificationInt notification) {
        return new ResumeValidationResult(ResumeValidationOutcome.ALREADY_PROCESSED,
                ResumeValidationReason.PREPARE_ALREADY_PRESENT, notification, Collections.emptySet());
    }

    public static ResumeValidationResult notEligible(ResumeValidationReason reason, NotificationInt notification) {
        return new ResumeValidationResult(ResumeValidationOutcome.NOT_ELIGIBLE, reason,
                notification, Collections.emptySet());
    }

    public ResumeValidationResult withRelevantTimelineCategories(Set<TimelineElementCategoryInt> categories) {
        return new ResumeValidationResult(outcome, reason, notification, Set.copyOf(categories));
    }
}