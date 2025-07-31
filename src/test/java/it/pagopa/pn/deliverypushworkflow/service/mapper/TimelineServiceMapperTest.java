package it.pagopa.pn.deliverypushworkflow.service.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypushworkflow.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.*;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimelineServiceMapperTest {
    private final TimelineServiceMapper timelineServiceMapper;

    public TimelineServiceMapperTest() {
        SmartMapper smartMapper = new SmartMapper(new ObjectMapper());
        this.timelineServiceMapper = new TimelineServiceMapper(smartMapper);
    }

    @Test
    void toTimelineElementDetailsInt() {
        NotificationViewedDetails details = new NotificationViewedDetails()
                .categoryType("NOTIFICATION_VIEWED")
                .recIndex(5)
                .eventTimestamp(Instant.now())
                .raddType("FSU")
                .raddTransactionId("RADD123")
                .notificationCost(1000L);

        TimelineElementDetailsInt result = timelineServiceMapper.toTimelineElementDetailsInt(details, TimelineElementCategoryInt.NOTIFICATION_VIEWED);
        NotificationViewedDetailsInt notificationViewedDetailsInt = (NotificationViewedDetailsInt) result;

        assertNotNull(notificationViewedDetailsInt);
        assertEquals(details.getRecIndex(), notificationViewedDetailsInt.getRecIndex());
        assertEquals(details.getNotificationCost().intValue(), notificationViewedDetailsInt.getNotificationCost());
    }

    @Test
    void toTimelineElementDetailsIntCategory() {
        TimelineElement timelineElement = new TimelineElement()
                .iun("IUN12345")
                .elementId("ELEM001")
                .timestamp(Instant.now())
                .paId("PA_TEST")
                .legalFactsIds(new ArrayList<>())
                .category(TimelineCategory.NOTIFICATION_VIEWED)
                .details(new NotificationViewedDetails().categoryType("TEST").recIndex(0))
                .statusInfo(new StatusInfo().actual("DELIVERED").statusChangeTimestamp(Instant.now()).statusChanged(true))
                .notificationSentAt(Instant.now())
                .ingestionTimestamp(Instant.now())
                .eventTimestamp(Instant.now());

        TimelineElementCategoryInt category = TimelineElementCategoryInt.NOTIFICATION_VIEWED;
        TimelineElementDetailsInt result = timelineServiceMapper.toTimelineElementDetailsInt(timelineElement.getDetails(), category);
        assertNotNull(result);
    }

    @Test
    void getNewTimelineElement_mapsFieldsCorrectly() {
        // Arrange
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .iun("IUN_TEST")
                .elementId("ELEM_ID")
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .build();

        NotificationRecipientInt recipient1 = NotificationRecipientInt.builder().internalId("rec1").build();
        NotificationRecipientInt recipient2 = NotificationRecipientInt.builder().internalId("rec2").build();

        NotificationInt notificationInt = NotificationInt.builder()
                .iun("IUN_TEST")
                .paProtocolNumber("PROT_123")
                .sentAt(Instant.now())
                .recipients(List.of(recipient1, recipient2))
                .build();

        // Act
        NewTimelineElement result = timelineServiceMapper.getNewTimelineElement(timelineElementInternal, notificationInt);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getTimelineElement());
        assertNotNull(result.getNotificationInfo());
        assertEquals("IUN_TEST", result.getTimelineElement().getIun());
        assertEquals("PROT_123", result.getNotificationInfo().getPaProtocolNumber());
        assertEquals(2, result.getNotificationInfo().getNumberOfRecipients());
    }

    @Test
    void getNewTimelineElement_mapsFieldsCorrectly_withLegalFacts() {
        // Arrange
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .iun("IUN_TEST")
                .elementId("ELEM_ID")
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .legalFactsIds(List.of(LegalFactsIdInt.builder().category(LegalFactCategoryInt.RECIPIENT_ACCESS).build()))
                .build();

        NotificationRecipientInt recipient1 = NotificationRecipientInt.builder().internalId("rec1").build();
        NotificationRecipientInt recipient2 = NotificationRecipientInt.builder().internalId("rec2").build();

        NotificationInt notificationInt = NotificationInt.builder()
                .iun("IUN_TEST")
                .paProtocolNumber("PROT_123")
                .sentAt(Instant.now())
                .recipients(List.of(recipient1, recipient2))
                .build();

        // Act
        NewTimelineElement result = timelineServiceMapper.getNewTimelineElement(timelineElementInternal, notificationInt);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getTimelineElement());
        assertNotNull(result.getNotificationInfo());
        assertEquals("IUN_TEST", result.getTimelineElement().getIun());
        assertEquals("PROT_123", result.getNotificationInfo().getPaProtocolNumber());
        assertEquals(2, result.getNotificationInfo().getNumberOfRecipients());
        assertNotNull(result.getTimelineElement().getLegalFactsIds());
    }

    @Test
    void toTimelineElementDetailsInt_mapsFieldsCorrectly() {
        TimelineElementDetails details = new AnalogSuccessWorkflowDetails()
                .categoryType("ANALOG_SUCCESS_WORKFLOW")
                .recIndex(0);

        TimelineElement timelineElement = new TimelineElement()
                .details(details)
                .category(TimelineCategory.ANALOG_SUCCESS_WORKFLOW);

        TimelineElementCategoryInt category = TimelineElementCategoryInt.ANALOG_SUCCESS_WORKFLOW;

        Object result = timelineServiceMapper.toTimelineElementDetailsInt(timelineElement.getDetails(), category);

        assertNotNull(result);
        assertInstanceOf(AnalogSuccessWorkflowDetailsInt.class, result);
        AnalogSuccessWorkflowDetailsInt analogSuccessWorkflowDetailsInt = (AnalogSuccessWorkflowDetailsInt) result;
        assertEquals("ANALOG_SUCCESS_WORKFLOW", analogSuccessWorkflowDetailsInt.getCategoryType());
        assertEquals(0, analogSuccessWorkflowDetailsInt.getRecIndex());
    }

    @Test
    void toTimelineElementInternal_mapsFieldsCorrectly() {
        TimelineElementDetails details = new NotificationViewedDetails().categoryType("NOTIFICATION_VIEWED").recIndex(1);
        StatusInfo statusInfo = new StatusInfo().actual("DELIVERED").statusChangeTimestamp(Instant.now()).statusChanged(true);
        LegalFactsId legalFactsId = new LegalFactsId().category(LegalFactsId.CategoryEnum.ANALOG_DELIVERY);
        TimelineElement timelineElement = new TimelineElement()
                .iun("IUN_TEST")
                .elementId("ELEM_ID")
                .timestamp(Instant.now())
                .paId("PA_TEST")
                .legalFactsIds(List.of(legalFactsId))
                .category(TimelineCategory.NOTIFICATION_VIEWED)
                .details(details)
                .statusInfo(statusInfo)
                .notificationSentAt(Instant.now())
                .ingestionTimestamp(Instant.now())
                .eventTimestamp(Instant.now());

        TimelineElementInternal result = timelineServiceMapper.toTimelineElementInternal(timelineElement);

        assertNotNull(result);
        assertEquals("IUN_TEST", result.getIun());
        assertEquals("ELEM_ID", result.getElementId());
        assertEquals(TimelineElementCategoryInt.NOTIFICATION_VIEWED, result.getCategory());
        assertNotNull(result.getDetails());
        assertNotNull(result.getStatusInfo());
    }

}