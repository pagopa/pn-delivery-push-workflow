package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.resumepostpayment;

import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.BaseAnalogDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.RecipientRelatedTimelineElementDetails;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.ScheduleAnalogWorkflowDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW;
import static it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt.NOTIFICATION_TIMELINE_REWORKED;
import static it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt.PAYMENT;
import static it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE;
import static it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt.PREPARE_SIMPLE_REGISTERED_LETTER;
import static it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW;
import static it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt.SCHEDULE_REFINEMENT;
import static it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt.SEND_ANALOG_DOMICILE;
import static it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK;
import static it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt.SEND_ANALOG_TIMEOUT;

@Service
@RequiredArgsConstructor
public class ResumePostPaymentEligibilityService {
    private static final Duration DEFAULT_MINIMUM_DELAY_SCHEDULE_ANALOG_WORKFLOW = Duration.ofHours(10);

    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final Clock clock;
    private final PnDeliveryPushWorkflowConfigs configs;

    public ResumeValidationResult validate(ResumePostPaymentEvent event) {
        NotificationInt notification = notificationService.getNotificationByIun(event.getIun());

        if (!recipientExists(notification, event.getRecIndex())) {
            return ResumeValidationResult.notEligible(ResumeValidationReason.RECIPIENT_NOT_FOUND, notification);
        }

        Set<TimelineElementInternal> timeline = timelineService.getTimelineStrongly(event.getIun(), false);
        Set<TimelineElementCategoryInt> recipientTimelineCategories = timeline.stream()
                .filter(element -> isRelatedToRecipient(element, event.getRecIndex()))
                .map(TimelineElementInternal::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());

        if (isAlreadyProcessed(event, timeline)) {
            return ResumeValidationResult.alreadyProcessed(notification, recipientTimelineCategories);
        }
        ResumeValidationResult commonResult = validateCommonRules(
                event, notification, timeline, recipientTimelineCategories);
        if (commonResult != null) {
            return commonResult;
        }

        return switch (event.getResumeType()) {
            case FIRST_ATTEMPT -> validateFirstAttempt(
                    event, notification, timeline, recipientTimelineCategories);
            case SECOND_ATTEMPT -> validateSecondAttempt(
                    event, notification, timeline, recipientTimelineCategories);
            case SIMPLE_REGISTERED_LETTER -> validateSimpleRegisteredLetter(
                    event, notification, timeline, recipientTimelineCategories);
        };
    }

    private ResumeValidationResult validateCommonRules(ResumePostPaymentEvent event, NotificationInt notification,
                                                        Set<TimelineElementInternal> timeline,
                                                        Set<TimelineElementCategoryInt> recipientTimelineCategories) {
        if (!hasGenericRecipientCategory(timeline, PAYMENT, event.getRecIndex())) {
            return ResumeValidationResult.notEligible(
                    ResumeValidationReason.PAYMENT_NOT_FOUND, notification, recipientTimelineCategories);
        }
        if (timelineUtils.checkIsNotificationCancellationRequested(event.getIun())) { // Non esiste l'elemento in dp-workflow, non lo importiamo per questa casistica di gestione una tantum
            return ResumeValidationResult.notEligible(
                    ResumeValidationReason.NOTIFICATION_CANCELLED, notification, recipientTimelineCategories);
        }
        if (timelineUtils.checkIsNotificationViewed(event.getIun(), event.getRecIndex())) { // Non esiste l'elemento in dp-workflow, non lo importiamo per questa casistica di gestione una tantum
            return ResumeValidationResult.notEligible(
                    ResumeValidationReason.NOTIFICATION_VIEWED, notification, recipientTimelineCategories);
        }
        if (hasGenericRecipientCategory(timeline, NOTIFICATION_TIMELINE_REWORKED, event.getRecIndex())) {
            return ResumeValidationResult.notEligible(
                    ResumeValidationReason.NOTIFICATION_REWORKED, notification, recipientTimelineCategories);
        }
        return null;
    }

    private ResumeValidationResult validateFirstAttempt(ResumePostPaymentEvent event, NotificationInt notification,
                                                         Set<TimelineElementInternal> timeline,
                                                         Set<TimelineElementCategoryInt> recipientTimelineCategories) {
        ScheduleAnalogWorkflowDetailsInt schedule = timeline.stream()
                .filter(element -> element.getCategory() == SCHEDULE_ANALOG_WORKFLOW)
                .map(TimelineElementInternal::getDetails)
                .filter(ScheduleAnalogWorkflowDetailsInt.class::isInstance)
                .map(ScheduleAnalogWorkflowDetailsInt.class::cast)
                .filter(details -> details.getRecIndex() == event.getRecIndex())
                .findFirst()
                .orElse(null);
        if (schedule == null) {
            return ResumeValidationResult.notEligible(ResumeValidationReason.SCHEDULE_ANALOG_WORKFLOW_NOT_FOUND,
                    notification, recipientTimelineCategories);
        }
        Duration minimumDelay = configs.getResumPostPaymentMinimumDelayScheduleAnalogWorkflow();
        Instant latestEligibleSchedulingDate = clock.instant().minus(
            minimumDelay != null ? minimumDelay : DEFAULT_MINIMUM_DELAY_SCHEDULE_ANALOG_WORKFLOW);
        if (schedule.getSchedulingDate() == null || schedule.getSchedulingDate().isAfter(latestEligibleSchedulingDate)) {
            return ResumeValidationResult.notEligible(ResumeValidationReason.SCHEDULE_ANALOG_WORKFLOW_TOO_RECENT,
                    notification, recipientTimelineCategories);
        }
        return ResumeValidationResult.eligible(notification, recipientTimelineCategories);
    }

    private ResumeValidationResult validateSecondAttempt(ResumePostPaymentEvent event, NotificationInt notification,
                                                          Set<TimelineElementInternal> timeline,
                                                          Set<TimelineElementCategoryInt> recipientTimelineCategories) {
        if (!hasAnalogAttempt(timeline, SEND_ANALOG_DOMICILE, event.getRecIndex(), 0)) {
            return ResumeValidationResult.notEligible(ResumeValidationReason.FIRST_ANALOG_SEND_NOT_FOUND,
                    notification, recipientTimelineCategories);
        }
        boolean hasKoFeedback = timeline.stream()
                .filter(element -> element.getCategory() == SEND_ANALOG_FEEDBACK)
                .map(TimelineElementInternal::getDetails)
                .filter(SendAnalogFeedbackDetailsInt.class::isInstance)
                .map(SendAnalogFeedbackDetailsInt.class::cast)
                .anyMatch(details -> details.getRecIndex() == event.getRecIndex()
                        && Objects.equals(details.getSentAttemptMade(), 0)
                        && details.getResponseStatus() == ResponseStatusInt.KO);
        //Timeout e feedback ovviamente ha senso controllarli solo per secondo tentativo, in quanto riguarda il primo tentativo andato in timeout
        boolean hasTimeout = hasAnalogAttempt(timeline, SEND_ANALOG_TIMEOUT, event.getRecIndex(), 0);
        if (!hasKoFeedback && !hasTimeout) {
            return ResumeValidationResult.notEligible(ResumeValidationReason.SECOND_ATTEMPT_TRIGGER_NOT_FOUND,
                    notification, recipientTimelineCategories);
        }
        return ResumeValidationResult.eligible(notification, recipientTimelineCategories);
    }

    private ResumeValidationResult validateSimpleRegisteredLetter(ResumePostPaymentEvent event,
                                                                   NotificationInt notification,
                                                                   Set<TimelineElementInternal> timeline,
                                                                   Set<TimelineElementCategoryInt> recipientTimelineCategories) {
        if (!hasGenericRecipientCategory(timeline, DIGITAL_FAILURE_WORKFLOW, event.getRecIndex())) {
            return ResumeValidationResult.notEligible(ResumeValidationReason.DIGITAL_FAILURE_WORKFLOW_NOT_FOUND,
                    notification, recipientTimelineCategories);
        }
        if (!hasGenericRecipientCategory(timeline, SCHEDULE_REFINEMENT, event.getRecIndex())) {
            return ResumeValidationResult.notEligible(ResumeValidationReason.SCHEDULE_REFINEMENT_NOT_FOUND,
                    notification, recipientTimelineCategories);
        }

        return ResumeValidationResult.eligible(notification, recipientTimelineCategories);
    }

    private boolean isAlreadyProcessed(ResumePostPaymentEvent event, Set<TimelineElementInternal> timeline) {
        return switch (event.getResumeType()) {
            case FIRST_ATTEMPT -> hasAnalogAttempt(timeline, PREPARE_ANALOG_DOMICILE, event.getRecIndex(), 0);
            case SECOND_ATTEMPT -> hasAnalogAttempt(timeline, PREPARE_ANALOG_DOMICILE, event.getRecIndex(), 1);
            case SIMPLE_REGISTERED_LETTER ->
                    hasGenericRecipientCategory(timeline, PREPARE_SIMPLE_REGISTERED_LETTER, event.getRecIndex());
        };
    }

    private boolean recipientExists(NotificationInt notification, Integer recIndex) {
        return recIndex != null && recIndex >= 0 && notification.getRecipients() != null
                && recIndex < notification.getRecipients().size();
    }

    private boolean isRelatedToRecipient(TimelineElementInternal element, int recIndex) {
        return element.getDetails() instanceof RecipientRelatedTimelineElementDetails details
                && details.getRecIndex() == recIndex;
    }

    private boolean hasGenericRecipientCategory(Set<TimelineElementInternal> timeline,
                                                TimelineElementCategoryInt category,
                                                int recIndex) {
        return timeline.stream()
                .filter(element -> element.getCategory() == category)
                .map(TimelineElementInternal::getDetails)
                .filter(RecipientRelatedTimelineElementDetails.class::isInstance)
                .map(RecipientRelatedTimelineElementDetails.class::cast)
                .anyMatch(details -> details.getRecIndex() == recIndex);
    }

    private boolean hasAnalogAttempt(Set<TimelineElementInternal> timeline,
                                     TimelineElementCategoryInt category,
                                     int recIndex, int sentAttemptMade) {
        return timeline.stream()
                .filter(element -> element.getCategory() == category)
                .map(TimelineElementInternal::getDetails)
                .filter(BaseAnalogDetailsInt.class::isInstance)
                .map(BaseAnalogDetailsInt.class::cast)
                .anyMatch(details -> details.getRecIndex() == recIndex
                        && Objects.equals(details.getSentAttemptMade(), sentAttemptMade));
    }
}