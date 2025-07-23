package it.pagopa.pn.deliverypushworkflow.service.mapper;

import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypushworkflow.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.NotificationViewedDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimelineServiceMapperTest {

    @Test
    void toTimelineElementDetailsInt() {
        TimelineElementDetails details = new TimelineElementDetails()
                .categoryType("TEST_CATEGORY")
                .legalFactId("LF123")
                .recIndex(5)
                .notificationRequestId("REQ123")
                .paProtocolNumber("PROT456")
                .idempotenceToken("TOKEN789")
                .generatedAarUrl("http://test/aar")
                .completionWorkflowDate(Instant.now())
                .legalFactGenerationDate(Instant.now())
                .isAvailable(true)
                .attemptDate(Instant.now())
                .eventTimestamp(Instant.now())
                .raddType("FSU")
                .raddTransactionId("RADD123")
                .sourceChannel("WEB")
                .sourceChannelDetails("Dettagli")
                .notificationCost(1000L)
                .sentAttemptMade(2)
                .sendDate(Instant.now())
                .relatedFeedbackTimelineId("FB123")
                .requestTimelineId("REQTL123")
                .numberOfRecipients(3)
                .schedulingDate(Instant.now())
                .lastAttemptDate(Instant.now())
                .retryNumber(1)
                .nextSourceAttemptsMade(1)
                .nextLastAttemptMadeForSource(Instant.now())
                .isFirstSendRetry(false)
                .notificationDate(Instant.now())
                .deliveryFailureCause("Nessuna")
                .deliveryDetailCode("D01")
                .sendingReceipts(new ArrayList<>())
                .shouldRetry(false)
                .relatedRequestId("RELREQ123")
                .productType("RS")
                .analogCost(500)
                .numberOfPages(10)
                .envelopeWeight(20)
                .prepareRequestId("PREP123")
                .f24Attachments(new ArrayList<>())
                .vat(22)
                .attachments(new ArrayList<>())
                .sendRequestId("SENDREQ123")
                .registeredLetterCode("RL123")
                .foreignState("IT")
                .aarKey("AARKEY123")
                .reasonCode("RC01")
                .reason("Test Reason")
                .amount(1500)
                .creditorTaxId("77777777777")
                .noticeCode("302000100000019421")
                .paymentSourceChannel("WEB")
                .uncertainPaymentDate(false)
                .schedulingAnalogDate(Instant.now())
                .cancellationRequestId("CANC123")
                .notRefinedRecipientIndexes(Arrays.asList(1, 2, 3))
                .failureCause("D00")
                .recIndexes(Arrays.asList(1, 2, 3))
                .registry("ANPR")
                .status("OK");

        TimelineElementDetailsInt result = TimelineServiceMapper.toTimelineElementDetailsInt(details, TimelineElementCategoryInt.NOTIFICATION_VIEWED);
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
                .details(new TimelineElementDetails().categoryType("TEST").legalFactId("LFID001"))
                .statusInfo(new StatusInfo().actual("DELIVERED").statusChangeTimestamp(Instant.now()).statusChanged(true))
                .notificationSentAt(Instant.now())
                .ingestionTimestamp(Instant.now())
                .eventTimestamp(Instant.now());

        TimelineElementCategoryInt category = TimelineElementCategoryInt.NOTIFICATION_VIEWED;
        TimelineElementDetailsInt result = TimelineServiceMapper.toTimelineElementDetailsInt(timelineElement.getDetails(), category);
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
        NewTimelineElement result = TimelineServiceMapper.getNewTimelineElement(timelineElementInternal, notificationInt);

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
        NewTimelineElement result = TimelineServiceMapper.getNewTimelineElement(timelineElementInternal, notificationInt);

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
        TimelineElementDetails details = new TimelineElementDetails()
                .categoryType("TEST_CATEGORY")
                .recIndex(5);

        TimelineElement timelineElement = new TimelineElement()
                .details(details)
                .category(TimelineCategory.NOTIFICATION_VIEWED);

        TimelineElementCategoryInt category = TimelineElementCategoryInt.NOTIFICATION_VIEWED;

        Object result = TimelineServiceMapper.toTimelineElementDetailsInt(timelineElement.getDetails(), category);

        assertNotNull(result);
        assertInstanceOf(NotificationViewedDetailsInt.class, result);
        NotificationViewedDetailsInt viewedDetails = (NotificationViewedDetailsInt) result;
        assertEquals(5, viewedDetails.getRecIndex());
    }

    @Test
    void toTimelineElementInternal_mapsFieldsCorrectly() {
        TimelineElementDetails details = new TimelineElementDetails().categoryType("NOTIFICATION_VIEWED").recIndex(1);
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

        TimelineElementInternal result = TimelineServiceMapper.toTimelineElementInternal(timelineElement);

        assertNotNull(result);
        assertEquals("IUN_TEST", result.getIun());
        assertEquals("ELEM_ID", result.getElementId());
        assertEquals(TimelineElementCategoryInt.NOTIFICATION_VIEWED, result.getCategory());
        assertNotNull(result.getDetails());
        assertNotNull(result.getStatusInfo());
    }

}