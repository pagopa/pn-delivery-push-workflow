package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.resumepostpayment;

import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;

import java.util.Collections;
import java.util.Set;

public record ResumeValidationResult(
        ResumeValidationOutcome outcome,
        ResumeValidationReason reason,
        NotificationInt notification,
        Set<TimelineElementCategoryInt> recipientTimelineCategories
) {
    public static ResumeValidationResult eligible(NotificationInt notification) {
        return eligible(notification, Collections.emptySet());
    }

    public static ResumeValidationResult eligible(NotificationInt notification,
                                                  Set<TimelineElementCategoryInt> recipientTimelineCategories) {
        return new ResumeValidationResult(ResumeValidationOutcome.ELIGIBLE, ResumeValidationReason.VALID,
                notification, Set.copyOf(recipientTimelineCategories));
    }

    public static ResumeValidationResult alreadyProcessed(NotificationInt notification,
                                                          Set<TimelineElementCategoryInt> recipientTimelineCategories) {
        return new ResumeValidationResult(ResumeValidationOutcome.ALREADY_PROCESSED,
                ResumeValidationReason.PREPARE_ALREADY_PRESENT, notification,
                Set.copyOf(recipientTimelineCategories));
    }

    public static ResumeValidationResult notEligible(ResumeValidationReason reason, NotificationInt notification) {
        return notEligible(reason, notification, Collections.emptySet());
    }

    public static ResumeValidationResult notEligible(ResumeValidationReason reason, NotificationInt notification,
                                                     Set<TimelineElementCategoryInt> recipientTimelineCategories) {
        return new ResumeValidationResult(ResumeValidationOutcome.NOT_ELIGIBLE, reason,
                notification, Set.copyOf(recipientTimelineCategories));
    }
}