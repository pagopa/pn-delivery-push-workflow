package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.resumepostpayment;

import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ResumePostPaymentEligibilityServiceTest {
    private static final String IUN = "IUN_01";
    private static final int REC_INDEX = 0;
    private static final Instant NOW = Instant.parse("2026-08-31T10:00:00Z");

    @Mock
    private NotificationService notificationService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;

    private ResumePostPaymentEligibilityService service;
    private NotificationInt notification;

    @BeforeEach
    void setUp() {
        notification = notification(true);
        service = new ResumePostPaymentEligibilityService(
                notificationService,
                timelineService,
                timelineUtils,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        Mockito.lenient().when(notificationService.getNotificationByIun(IUN)).thenReturn(notification);
    }

    @Test
    void firstAttemptIsEligibleAtMinimumDelayBoundary() {
        Set<TimelineElementInternal> timeline = commonTimeline();
        timeline.add(scheduleAnalog(NOW.minusSeconds(10 * 60 * 60)));
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(timeline);

        ResumeValidationResult result = service.validate(event(ResumeType.FIRST_ATTEMPT));

        assertResult(result, ResumeValidationOutcome.ELIGIBLE, ResumeValidationReason.VALID);
        Mockito.verify(timelineService).getTimelineStrongly(IUN, false);
    }

    @Test
    void firstAttemptIsNotEligibleWhenScheduleIsTooRecent() {
        Set<TimelineElementInternal> timeline = commonTimeline();
        timeline.add(scheduleAnalog(NOW.minusSeconds(9 * 60 * 60)));
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(timeline);

        ResumeValidationResult result = service.validate(event(ResumeType.FIRST_ATTEMPT));

        assertResult(result, ResumeValidationOutcome.NOT_ELIGIBLE,
                ResumeValidationReason.SCHEDULE_ANALOG_WORKFLOW_TOO_RECENT);
    }

    @Test
    void firstAttemptWithoutScheduleIsNotEligible() {
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(commonTimeline());

        ResumeValidationResult result = service.validate(event(ResumeType.FIRST_ATTEMPT));

        assertResult(result, ResumeValidationOutcome.NOT_ELIGIBLE,
                ResumeValidationReason.SCHEDULE_ANALOG_WORKFLOW_NOT_FOUND);
    }

    @Test
    void secondAttemptIsEligibleWithKoFeedback() {
        Set<TimelineElementInternal> timeline = commonTimeline();
        timeline.add(analogElement(SEND_ANALOG_DOMICILE, 0));
        timeline.add(koFeedback());
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(timeline);

        ResumeValidationResult result = service.validate(event(ResumeType.SECOND_ATTEMPT));

        assertResult(result, ResumeValidationOutcome.ELIGIBLE, ResumeValidationReason.VALID);
    }

    @Test
    void secondAttemptIsEligibleWithTimeout() {
        Set<TimelineElementInternal> timeline = commonTimeline();
        timeline.add(analogElement(SEND_ANALOG_DOMICILE, 0));
        timeline.add(analogElement(SEND_ANALOG_TIMEOUT, 0));
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(timeline);

        ResumeValidationResult result = service.validate(event(ResumeType.SECOND_ATTEMPT));

        assertResult(result, ResumeValidationOutcome.ELIGIBLE, ResumeValidationReason.VALID);
    }

    @Test
    void secondAttemptWithoutFirstSendIsNotEligible() {
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(commonTimeline());

        ResumeValidationResult result = service.validate(event(ResumeType.SECOND_ATTEMPT));

        assertResult(result, ResumeValidationOutcome.NOT_ELIGIBLE,
                ResumeValidationReason.FIRST_ANALOG_SEND_NOT_FOUND);
    }

    @Test
    void simpleRegisteredLetterIsEligible() {
        Set<TimelineElementInternal> timeline = commonTimeline();
        timeline.add(recipientElement(DIGITAL_FAILURE_WORKFLOW));
        timeline.add(recipientElement(SCHEDULE_REFINEMENT));
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(timeline);

        ResumeValidationResult result = service.validate(event(ResumeType.SIMPLE_REGISTERED_LETTER));

        assertResult(result, ResumeValidationOutcome.ELIGIBLE, ResumeValidationReason.VALID);
    }

    @ParameterizedTest
    @EnumSource(ResumeType.class)
    void prepareAlreadyPresentIsAlreadyProcessed(ResumeType resumeType) {
        Set<TimelineElementInternal> timeline = commonTimeline();
        switch (resumeType) {
            case FIRST_ATTEMPT -> timeline.add(analogElement(PREPARE_ANALOG_DOMICILE, 0));
            case SECOND_ATTEMPT -> timeline.add(analogElement(PREPARE_ANALOG_DOMICILE, 1));
            case SIMPLE_REGISTERED_LETTER -> timeline.add(recipientElement(PREPARE_SIMPLE_REGISTERED_LETTER));
        }
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(timeline);

        ResumeValidationResult result = service.validate(event(resumeType));

        assertResult(result, ResumeValidationOutcome.ALREADY_PROCESSED,
                ResumeValidationReason.PREPARE_ALREADY_PRESENT);
    }

    @Test
    void prepareAlreadyPresentTakesPrecedenceOverCommonValidation() {
        Set<TimelineElementInternal> timeline = new HashSet<>();
        timeline.add(analogElement(PREPARE_ANALOG_DOMICILE, 0));
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(timeline);

        ResumeValidationResult result = service.validate(event(ResumeType.FIRST_ATTEMPT));

        assertResult(result, ResumeValidationOutcome.ALREADY_PROCESSED,
                ResumeValidationReason.PREPARE_ALREADY_PRESENT);
        Mockito.verifyNoInteractions(timelineUtils);
    }

    @Test
    void missingPaymentIsNotEligible() {
        Set<TimelineElementInternal> timeline = new HashSet<>();
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(timeline);

        ResumeValidationResult result = service.validate(event(ResumeType.FIRST_ATTEMPT));

        assertResult(result, ResumeValidationOutcome.NOT_ELIGIBLE, ResumeValidationReason.PAYMENT_NOT_FOUND);
    }

    @Test
    void paymentForAnotherRecipientIsNotEligible() {
        Set<TimelineElementInternal> timeline = new HashSet<>(Set.of(recipientElement(PAYMENT, 1)));
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(timeline);

        ResumeValidationResult result = service.validate(event(ResumeType.FIRST_ATTEMPT));

        assertResult(result, ResumeValidationOutcome.NOT_ELIGIBLE, ResumeValidationReason.PAYMENT_NOT_FOUND);
    }

    @Test
    void cancelledNotificationIsNotEligible() {
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(commonTimeline());
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(IUN)).thenReturn(true);

        ResumeValidationResult result = service.validate(event(ResumeType.FIRST_ATTEMPT));

        assertResult(result, ResumeValidationOutcome.NOT_ELIGIBLE, ResumeValidationReason.NOTIFICATION_CANCELLED);
    }

    @Test
    void viewedNotificationIsNotEligible() {
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(commonTimeline());
        Mockito.when(timelineUtils.checkIsNotificationViewed(IUN, REC_INDEX)).thenReturn(true);

        ResumeValidationResult result = service.validate(event(ResumeType.FIRST_ATTEMPT));

        assertResult(result, ResumeValidationOutcome.NOT_ELIGIBLE, ResumeValidationReason.NOTIFICATION_VIEWED);
    }

    @Test
    void reworkedRecipientIsNotEligible() {
        Set<TimelineElementInternal> timeline = commonTimeline();
        timeline.add(recipientElement(NOTIFICATION_TIMELINE_REWORKED));
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(timeline);

        ResumeValidationResult result = service.validate(event(ResumeType.FIRST_ATTEMPT));

        assertResult(result, ResumeValidationOutcome.NOT_ELIGIBLE, ResumeValidationReason.NOTIFICATION_REWORKED);
    }

    @Test
    void unknownRecipientIsNotEligibleWithoutReadingTimeline() {
        ResumePostPaymentEvent event = ResumePostPaymentEvent.builder()
                .iun(IUN)
                .recIndex(1)
                .resumeType(ResumeType.FIRST_ATTEMPT)
                .build();

        ResumeValidationResult result = service.validate(event);

        assertResult(result, ResumeValidationOutcome.NOT_ELIGIBLE, ResumeValidationReason.RECIPIENT_NOT_FOUND);
        Mockito.verifyNoInteractions(timelineService, timelineUtils);
    }

    @Test
    void notificationIunMismatchIsNotEligible() {
        notification = NotificationInt.builder()
                .iun("DIFFERENT_IUN")
                .recipients(notification.getRecipients())
            .build();
        Mockito.when(notificationService.getNotificationByIun(IUN)).thenReturn(notification);

        ResumeValidationResult result = service.validate(event(ResumeType.FIRST_ATTEMPT));

        assertResult(result, ResumeValidationOutcome.NOT_ELIGIBLE,
                ResumeValidationReason.NOTIFICATION_IUN_MISMATCH);
        Mockito.verifyNoInteractions(timelineService, timelineUtils);
    }

    @Test
    void secondAttemptWithoutKoOrTimeoutIsNotEligible() {
        Set<TimelineElementInternal> timeline = commonTimeline();
        timeline.add(analogElement(SEND_ANALOG_DOMICILE, 0));
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(timeline);

        ResumeValidationResult result = service.validate(event(ResumeType.SECOND_ATTEMPT));

        assertResult(result, ResumeValidationOutcome.NOT_ELIGIBLE,
                ResumeValidationReason.SECOND_ATTEMPT_TRIGGER_NOT_FOUND);
    }

    @Test
    void simpleRegisteredLetterWithoutPhysicalAddressIsNotEligible() {
        notification = notification(false);
        Mockito.when(notificationService.getNotificationByIun(IUN)).thenReturn(notification);
        Set<TimelineElementInternal> timeline = commonTimeline();
        timeline.add(recipientElement(DIGITAL_FAILURE_WORKFLOW));
        timeline.add(recipientElement(SCHEDULE_REFINEMENT));
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(timeline);

        ResumeValidationResult result = service.validate(event(ResumeType.SIMPLE_REGISTERED_LETTER));

        assertResult(result, ResumeValidationOutcome.NOT_ELIGIBLE,
                ResumeValidationReason.PHYSICAL_ADDRESS_NOT_FOUND);
    }

    @Test
    void simpleRegisteredLetterWithoutDigitalFailureIsNotEligible() {
        Set<TimelineElementInternal> timeline = commonTimeline();
        timeline.add(recipientElement(SCHEDULE_REFINEMENT));
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(timeline);

        ResumeValidationResult result = service.validate(event(ResumeType.SIMPLE_REGISTERED_LETTER));

        assertResult(result, ResumeValidationOutcome.NOT_ELIGIBLE,
                ResumeValidationReason.DIGITAL_FAILURE_WORKFLOW_NOT_FOUND);
    }

    @Test
    void simpleRegisteredLetterWithoutScheduleRefinementIsNotEligible() {
        Set<TimelineElementInternal> timeline = commonTimeline();
        timeline.add(recipientElement(DIGITAL_FAILURE_WORKFLOW));
        Mockito.when(timelineService.getTimelineStrongly(IUN, false)).thenReturn(timeline);

        ResumeValidationResult result = service.validate(event(ResumeType.SIMPLE_REGISTERED_LETTER));

        assertResult(result, ResumeValidationOutcome.NOT_ELIGIBLE,
                ResumeValidationReason.SCHEDULE_REFINEMENT_NOT_FOUND);
    }

    private Set<TimelineElementInternal> commonTimeline() {
        return new HashSet<>(Set.of(recipientElement(PAYMENT)));
    }

    private TimelineElementInternal recipientElement(TimelineElementCategoryInt category) {
        return recipientElement(category, REC_INDEX);
    }

    private TimelineElementInternal recipientElement(TimelineElementCategoryInt category, int recIndex) {
        RecipientRelatedTimelineElementDetails details = Mockito.mock(RecipientRelatedTimelineElementDetails.class);
        Mockito.lenient().when(details.getRecIndex()).thenReturn(recIndex);
        return element(category, details);
    }

    private TimelineElementInternal scheduleAnalog(Instant schedulingDate) {
        return element(SCHEDULE_ANALOG_WORKFLOW, ScheduleAnalogWorkflowDetailsInt.builder()
                .recIndex(REC_INDEX)
                .schedulingDate(schedulingDate)
                .build());
    }

    private TimelineElementInternal analogElement(TimelineElementCategoryInt category, int attempt) {
        return element(category, BaseAnalogDetailsInt.builder()
                .recIndex(REC_INDEX)
                .sentAttemptMade(attempt)
                .build());
    }

    private TimelineElementInternal koFeedback() {
        return element(SEND_ANALOG_FEEDBACK, SendAnalogFeedbackDetailsInt.builder()
                .recIndex(REC_INDEX)
                .sentAttemptMade(0)
                .responseStatus(ResponseStatusInt.KO)
                .build());
    }

    private TimelineElementInternal element(TimelineElementCategoryInt category, Object details) {
        return TimelineElementInternal.builder()
                .iun(IUN)
                .elementId(category + "_" + System.identityHashCode(details))
                .timestamp(NOW)
                .category(category)
                .details((it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementDetailsInt) details)
                .build();
    }

    private NotificationInt notification(boolean withPhysicalAddress) {
        NotificationRecipientInt recipient = NotificationRecipientInt.builder()
                .physicalAddress(withPhysicalAddress ? PhysicalAddressInt.builder().address("Via Roma 1").build() : null)
                .build();
        return NotificationInt.builder()
                .iun(IUN)
                .recipients(List.of(recipient))
                .build();
    }

    private ResumePostPaymentEvent event(ResumeType resumeType) {
        return ResumePostPaymentEvent.builder()
                .iun(IUN)
                .recIndex(REC_INDEX)
                .resumeType(resumeType)
                .build();
    }

    private void assertResult(ResumeValidationResult result, ResumeValidationOutcome outcome,
                              ResumeValidationReason reason) {
        assertEquals(outcome, result.outcome());
        assertEquals(reason, result.reason());
        assertEquals(notification, result.notification());
    }
}