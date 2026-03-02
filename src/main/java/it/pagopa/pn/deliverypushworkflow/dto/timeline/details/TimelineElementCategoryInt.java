package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import lombok.Getter;

@Getter
public enum TimelineElementCategoryInt {
    GENERATED_F24(GeneratedF24DetailsInt.class, TimelineElementCategoryInt.VERSION_23),
    SEND_COURTESY_MESSAGE(SendCourtesyMessageDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    GET_ADDRESS(GetAddressInfoDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    PUBLIC_REGISTRY_CALL(PublicRegistryCallDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    PUBLIC_REGISTRY_RESPONSE(PublicRegistryResponseDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    SCHEDULE_ANALOG_WORKFLOW(ScheduleAnalogWorkflowDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    SCHEDULE_DIGITAL_WORKFLOW(ScheduleDigitalWorkflowDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    PREPARE_DIGITAL_DOMICILE(PrepareDigitalDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    SEND_DIGITAL_DOMICILE(SendDigitalDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    SEND_DIGITAL_FEEDBACK(SendDigitalFeedbackDetailsInt.class, TimelineElementCategoryInt.PRIORITY_AFTER, TimelineElementCategoryInt.VERSION_10),
    SEND_DIGITAL_PROGRESS(SendDigitalProgressDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    REFINEMENT(RefinementDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    SCHEDULE_REFINEMENT(ScheduleRefinementDetailsInt.class, TimelineElementCategoryInt.PRIORITY_SCHEDULE_REFINEMENT, TimelineElementCategoryInt.VERSION_10),
    DIGITAL_DELIVERY_CREATION_REQUEST(DigitalDeliveryCreationRequestDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    DIGITAL_SUCCESS_WORKFLOW(DigitalSuccessWorkflowDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    DIGITAL_FAILURE_WORKFLOW(DigitalFailureWorkflowDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    ANALOG_SUCCESS_WORKFLOW(AnalogSuccessWorkflowDetailsInt.class, TimelineElementCategoryInt.PRIORITY_ANALOG_SUCCESS_WORKFLOW, TimelineElementCategoryInt.VERSION_10),
    ANALOG_FAILURE_WORKFLOW(AnalogFailureWorkflowDetailsInt.class, TimelineElementCategoryInt.PRIORITY_ANALOG_FAILURE_WORKFLOW, TimelineElementCategoryInt.VERSION_10),
    COMPLETELY_UNREACHABLE_CREATION_REQUEST(CompletelyUnreachableCreationRequestDetails.class, TimelineElementCategoryInt.PRIORITY_COMPLETELY_UNREACHABLE_CREATION_REQUEST, TimelineElementCategoryInt.VERSION_10),
    PREPARE_SIMPLE_REGISTERED_LETTER(BaseRegisteredLetterDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    SEND_SIMPLE_REGISTERED_LETTER(SimpleRegisteredLetterDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    NOTIFICATION_VIEWED_CREATION_REQUEST(NotificationViewedCreationRequestDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    NOTIFICATION_VIEWED(NotificationViewedDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    PREPARE_ANALOG_DOMICILE(BaseAnalogDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    PREPARE_ANALOG_DOMICILE_FAILURE(PrepareAnalogDomicileFailureDetailsInt.class, TimelineElementCategoryInt.VERSION_20),
    SEND_ANALOG_DOMICILE(SendAnalogDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    SEND_ANALOG_PROGRESS(SendAnalogProgressDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    SEND_ANALOG_FEEDBACK(SendAnalogFeedbackDetailsInt.class, TimelineElementCategoryInt.PRIORITY_SEND_ANALOG_FEEDBACK, TimelineElementCategoryInt.VERSION_10),
    PAYMENT(NotificationPaidDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    COMPLETELY_UNREACHABLE(CompletelyUnreachableDetailsInt.class, TimelineElementCategoryInt.PRIORITY_COMPLETELY_UNREACHABLET, TimelineElementCategoryInt.VERSION_10),
    AAR_CREATION_REQUEST(AarCreationRequestDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    AAR_GENERATION(AarGenerationDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    NOT_HANDLED(NotHandledDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    SEND_SIMPLE_REGISTERED_LETTER_PROGRESS(SimpleRegisteredLetterProgressDetailsInt.class, TimelineElementCategoryInt.VERSION_10),
    PROBABLE_SCHEDULING_ANALOG_DATE(ProbableDateAnalogWorkflowDetailsInt.class, TimelineElementCategoryInt.VERSION_20),
    NOTIFICATION_CANCELLED(NotificationCancelledDetailsInt.class, TimelineElementCategoryInt.VERSION_20),
    NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST(NotificationCancelledDocumentCreationRequestDetailsInt.class, TimelineElementCategoryInt.VERSION_25),
    ANALOG_WORKFLOW_RECIPIENT_DECEASED(AnalogWorfklowRecipientDeceasedDetailsInt.class, TimelineElementCategoryInt.PRIORITY_ANALOG_WORKFLOW_RECIPIENT_DECEASED, TimelineElementCategoryInt.VERSION_26),
    SEND_ANALOG_TIMEOUT_CREATION_REQUEST(SendAnalogTimeoutCreationRequestDetailsInt.class, TimelineElementCategoryInt.VERSION_28),
    SEND_ANALOG_TIMEOUT(SendAnalogTimeoutDetailsInt.class, TimelineElementCategoryInt.VERSION_28),
    ANALOG_FAILURE_WORKFLOW_TIMEOUT(AnalogFailureWorkflowTimeoutDetailsInt.class, TimelineElementCategoryInt.PRIORITY_ANALOG_FAILURE_WORKFLOW_TIMEOUT, TimelineElementCategoryInt.VERSION_28),
    NOTIFICATION_TIMELINE_REWORKED(NotificationTimelineReworkedDetailsInt.class, TimelineElementCategoryInt.PRIORITY_NOTIFICATION_TIMELINE_REWORKED, TimelineElementCategoryInt.VERSION_28);

    private final Class<? extends TimelineElementDetailsInt> detailsJavaClass;
    private final int priority;
    private final int version;

    public static final int PRIORITY_BEFORE = 10;
    public static final int PRIORITY_AFTER = 20;

    public static final int PRIORITY_SEND_ANALOG_FEEDBACK = 30;
    public static final int PRIORITY_ANALOG_SUCCESS_WORKFLOW = 40;
    public static final int PRIORITY_ANALOG_FAILURE_WORKFLOW = 40;
    public static final int PRIORITY_ANALOG_WORKFLOW_RECIPIENT_DECEASED = 40;
    public static final int PRIORITY_COMPLETELY_UNREACHABLE_CREATION_REQUEST = 50;
    public static final int PRIORITY_COMPLETELY_UNREACHABLET = 60;
    public static final int PRIORITY_SCHEDULE_REFINEMENT = 70;
    public static final int PRIORITY_ANALOG_FAILURE_WORKFLOW_TIMEOUT = 40;
    public static final int PRIORITY_NOTIFICATION_TIMELINE_REWORKED = PRIORITY_AFTER;

    public static final int VERSION_10 = 10;
    public static final int VERSION_20 = 20;
    public static final int VERSION_23 = 23;
    public static final int VERSION_25 = 25;
    public static final int VERSION_26 = 26;
    public static final int VERSION_28 = 28;

    TimelineElementCategoryInt(Class<? extends TimelineElementDetailsInt> detailsJavaClass, int version) {
        this(detailsJavaClass, PRIORITY_BEFORE, version);
    }


    TimelineElementCategoryInt(Class<? extends TimelineElementDetailsInt> detailsJavaClass, int priority, int version) {
        this.detailsJavaClass = detailsJavaClass;
        this.priority = priority;
        this.version = version;
    }
    
    public Class<? extends TimelineElementDetailsInt> getDetailsJavaClass() {
        return this.detailsJavaClass;
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
