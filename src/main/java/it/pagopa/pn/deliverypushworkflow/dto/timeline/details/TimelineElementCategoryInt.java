package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import lombok.Getter;

@Getter
public enum TimelineElementCategoryInt {
    GENERATED_F24(GeneratedF24DetailsInt.class),
    SEND_COURTESY_MESSAGE(SendCourtesyMessageDetailsInt.class),
    GET_ADDRESS(GetAddressInfoDetailsInt.class),
    PUBLIC_REGISTRY_CALL(PublicRegistryCallDetailsInt.class),
    PUBLIC_REGISTRY_RESPONSE(PublicRegistryResponseDetailsInt.class),
    SCHEDULE_ANALOG_WORKFLOW(ScheduleAnalogWorkflowDetailsInt.class),
    SCHEDULE_DIGITAL_WORKFLOW(ScheduleDigitalWorkflowDetailsInt.class),
    PREPARE_DIGITAL_DOMICILE(PrepareDigitalDetailsInt.class),
    SEND_DIGITAL_DOMICILE(SendDigitalDetailsInt.class),
    SEND_DIGITAL_FEEDBACK(SendDigitalFeedbackDetailsInt.class),
    SEND_DIGITAL_PROGRESS(SendDigitalProgressDetailsInt.class),
    REFINEMENT(RefinementDetailsInt.class),
    SCHEDULE_REFINEMENT(ScheduleRefinementDetailsInt.class),
    DIGITAL_DELIVERY_CREATION_REQUEST(DigitalDeliveryCreationRequestDetailsInt.class),
    DIGITAL_SUCCESS_WORKFLOW(DigitalSuccessWorkflowDetailsInt.class),
    DIGITAL_FAILURE_WORKFLOW(DigitalFailureWorkflowDetailsInt.class),
    ANALOG_SUCCESS_WORKFLOW(AnalogSuccessWorkflowDetailsInt.class),
    ANALOG_FAILURE_WORKFLOW(AnalogFailureWorkflowDetailsInt.class),
    COMPLETELY_UNREACHABLE_CREATION_REQUEST(CompletelyUnreachableCreationRequestDetails.class),
    PREPARE_SIMPLE_REGISTERED_LETTER(BaseRegisteredLetterDetailsInt.class),
    SEND_SIMPLE_REGISTERED_LETTER(SimpleRegisteredLetterDetailsInt.class),
    NOTIFICATION_VIEWED_CREATION_REQUEST(NotificationViewedCreationRequestDetailsInt.class),
    NOTIFICATION_VIEWED(NotificationViewedDetailsInt.class),
    PREPARE_ANALOG_DOMICILE(BaseAnalogDetailsInt.class),
    PREPARE_ANALOG_DOMICILE_FAILURE(PrepareAnalogDomicileFailureDetailsInt.class),
    SEND_ANALOG_DOMICILE(SendAnalogDetailsInt.class),
    SEND_ANALOG_PROGRESS(SendAnalogProgressDetailsInt.class),
    SEND_ANALOG_FEEDBACK(SendAnalogFeedbackDetailsInt.class),
    PAYMENT(NotificationPaidDetailsInt.class),
    COMPLETELY_UNREACHABLE(CompletelyUnreachableDetailsInt.class),
    AAR_CREATION_REQUEST(AarCreationRequestDetailsInt.class),
    AAR_GENERATION(AarGenerationDetailsInt.class),
    NOT_HANDLED(NotHandledDetailsInt.class),
    SEND_SIMPLE_REGISTERED_LETTER_PROGRESS(SimpleRegisteredLetterProgressDetailsInt.class),
    PROBABLE_SCHEDULING_ANALOG_DATE(ProbableDateAnalogWorkflowDetailsInt.class),
    NOTIFICATION_CANCELLED(NotificationCancelledDetailsInt.class),
    NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST(NotificationCancelledDocumentCreationRequestDetailsInt.class),
    ANALOG_WORKFLOW_RECIPIENT_DECEASED(AnalogWorfklowRecipientDeceasedDetailsInt.class),
    SEND_ANALOG_TIMEOUT_CREATION_REQUEST(SendAnalogTimeoutCreationRequestDetailsInt.class),
    SEND_ANALOG_TIMEOUT(SendAnalogTimeoutDetailsInt.class),
    ANALOG_FAILURE_WORKFLOW_TIMEOUT(AnalogFailureWorkflowTimeoutDetailsInt.class),
    NOTIFICATION_TIMELINE_REWORKED(NotificationTimelineReworkedDetailsInt.class),
    COURTESY_CHANNEL_FAILED(CourtesyChannelFailedDetailsInt.class),
    PUBLIC_REGISTRY_VALIDATION_RESPONSE(PublicRegistryValidationResponseDetailsInt.class);

    private final Class<? extends TimelineElementDetailsInt> detailsJavaClass;


    TimelineElementCategoryInt(Class<? extends TimelineElementDetailsInt> detailsJavaClass) {
        this.detailsJavaClass = detailsJavaClass;
    }

    /**
     * Checks if the given category is a known TimelineElementCategoryInt.
     *
     * @param category the category to check
     * @return true if the category is known, false otherwise
     */
    public static boolean isKnownCategory(String category) {
        try {
            TimelineElementCategoryInt.valueOf(category);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
