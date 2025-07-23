package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NewTimelineElement;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.TimelineCategory;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.TimelineElement;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.TimelineElementDetails;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.timeline.TimelineClient;
import it.pagopa.pn.deliverypushworkflow.service.mapper.TimelineServiceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TimelineServiceHttpImplTest {

    @Mock
    private TimelineClient timelineClient;

    @InjectMocks
    private TimelineServiceHttpImpl timelineServiceHttp;

    @Test
    void addTimelineElement() {
        TimelineElementInternal element = getTimelineElementInternal();
        NotificationInt notification = new NotificationInt();

        Mockito.when(timelineClient.addTimelineElement(Mockito.any(NewTimelineElement.class))).thenReturn(true);

        boolean result = timelineServiceHttp.addTimelineElement(element, notification);

        assertTrue(result);
    }

    @Test
    void retrieveAndIncrementCounterForTimelineEvent() {
        String timelineId = "timeline123";
        Long expectedCounter = 42L;

        Mockito.when(timelineClient.retrieveAndIncrementCounterForTimelineEvent(Mockito.anyString())).thenReturn(expectedCounter);

        Long result = timelineServiceHttp.retrieveAndIncrementCounterForTimelineEvent(timelineId);

        assertEquals(expectedCounter, result);
    }

    @Test
    void retrieveAndIncrementCounterForTimelineEventReturnsNull() {
        String timelineId = "timeline123";
        Mockito.when(timelineClient.retrieveAndIncrementCounterForTimelineEvent(Mockito.anyString())).thenReturn(null);

        Long result = timelineServiceHttp.retrieveAndIncrementCounterForTimelineEvent(timelineId);

        assertNull(result);
    }

    @Test
    void getTimelineElementReturnsMappedElement() {
        String iun = "iun123";
        String timelineId = "timeline123";
        TimelineElement timelineElement = new TimelineElement();
        TimelineElementInternal expectedElement = new TimelineElementInternal();

        Mockito.when(timelineClient.getTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(timelineElement);

        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toTimelineElementInternal(Mockito.any()))
                    .thenReturn(expectedElement);

            Optional<TimelineElementInternal> result = timelineServiceHttp.getTimelineElement(iun, timelineId);

            assertTrue(result.isPresent());
            assertEquals(expectedElement, result.get());
        }
    }

    @Test
    void getTimelineElementStronglyReturnsMappedElement() {
        String iun = "iun123";
        String timelineId = "timeline123";
        TimelineElement timelineElement = new TimelineElement();
        TimelineElementInternal expectedElement = new TimelineElementInternal();

        Mockito.when(timelineClient.getTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(timelineElement);

        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toTimelineElementInternal(Mockito.any()))
                    .thenReturn(expectedElement);

            Optional<TimelineElementInternal> result = timelineServiceHttp.getTimelineElementStrongly(iun, timelineId);

            assertTrue(result.isPresent());
            assertEquals(expectedElement, result.get());
        }
    }

    @Test
    void getTimelineReturnsOnlyElementsWithKnownCategory() {
        String iun = "iun123";
        boolean confidentialInfoRequired = true;
        TimelineElement timelineElementWithKnownCategory = new TimelineElement();
        timelineElementWithKnownCategory.setCategory(TimelineCategory.NOTIFICATION_VIEWED); // Example of a known category
        TimelineElement timelineElementWithUnknownCategory = new TimelineElement();
        timelineElementWithUnknownCategory.setCategory(TimelineCategory.NORMALIZED_ADDRESS); // Example of an unknown category
        TimelineElementInternal mappedElement = new TimelineElementInternal();


        List<TimelineElement> timelineElements = new ArrayList<>();
        timelineElements.add(timelineElementWithKnownCategory);
        timelineElements.add(timelineElementWithUnknownCategory);

        Mockito.when(timelineClient.getTimeline(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any()))
                .thenReturn(timelineElements);

        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toTimelineElementInternal(Mockito.any()))
                    .thenReturn(mappedElement);

            Set<TimelineElementInternal> result = timelineServiceHttp.getTimeline(iun, confidentialInfoRequired);

            assertEquals(1, result.size()); // The unknown category should be filtered out
            assertTrue(result.contains(mappedElement));
        }
    }

    @Test
    void getTimelineReturnsEmptySetWhenClientReturnsNull() {
        String iun = "iun123";
        boolean confidentialInfoRequired = true;

        Mockito.when(timelineClient.getTimeline(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any()))
                .thenReturn(null);

        Set<TimelineElementInternal> result = timelineServiceHttp.getTimeline(iun, confidentialInfoRequired);

        assertTrue(result.isEmpty());
    }


    @Test
    void getTimelineElementDetailsReturnsMappedDetails() {
        String iun = "iun123";
        String timelineId = "timeline123";
        TimelineElementDetails timelineElementDetails = new TimelineElementDetails();
        TimelineElementCategoryInt category = TimelineElementCategoryInt.NOTIFICATION_VIEWED;
        TimelineElementDetailsInt mappedDetails = Mockito.mock(TimelineElementDetailsInt.class);

        Mockito.when(timelineClient.getTimelineElementDetails(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(timelineElementDetails);

        // Simula il categoryType nel TimelineElementDetails
        timelineElementDetails.setCategoryType(category.name());

        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toTimelineElementDetailsInt(
                    Mockito.any(), Mockito.any()))
                    .thenReturn(mappedDetails);

            Optional<TimelineElementDetailsInt> result = timelineServiceHttp.getTimelineElementDetails(iun, timelineId, TimelineElementDetailsInt.class);

            assertTrue(result.isPresent());
            assertEquals(mappedDetails, result.get());
        }
    }

    @Test
    void getTimelineElementDetailForSpecificRecipientReturnsMappedDetails() {
        String iun = "iun123";
        int recIndex = 0;
        boolean confidentialInfoRequired = true;
        TimelineElementCategoryInt category = TimelineElementCategoryInt.NOTIFICATION_VIEWED;
        TimelineElementDetails timelineElementDetails = new TimelineElementDetails();
        timelineElementDetails.setCategoryType(category.name());

        Mockito.when(timelineClient.getTimelineElementDetailForSpecificRecipient(
                iun,
                recIndex,
                confidentialInfoRequired,
                TimelineCategory.fromValue(category.name())
        )).thenReturn(timelineElementDetails);

        TimelineElementDetailsInt mappedDetails = Mockito.mock(TimelineElementDetailsInt.class);
        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toTimelineElementDetailsInt(
                    Mockito.eq(timelineElementDetails),
                    Mockito.eq(TimelineElementCategoryInt.valueOf(timelineElementDetails.getCategoryType()))
            )).thenReturn(mappedDetails);

            Optional<TimelineElementDetailsInt> result = timelineServiceHttp.getTimelineElementDetailForSpecificRecipient(
                    iun, recIndex, confidentialInfoRequired, category, TimelineElementDetailsInt.class);

            assertTrue(result.isPresent());
            assertEquals(mappedDetails, result.get());
        }
    }

    @Test
    void getTimelineElementForSpecificRecipientReturnsMappedElement() {
        String iun = "iun123";
        int recIndex = 1;
        TimelineElementCategoryInt category = TimelineElementCategoryInt.NOTIFICATION_VIEWED;
        TimelineElement timelineElement = new TimelineElement();
        TimelineElementInternal expectedElement = new TimelineElementInternal();

        Mockito.when(timelineClient.getTimelineElementForSpecificRecipient(
                iun,
                recIndex,
                TimelineCategory.fromValue(category.name())
        )).thenReturn(timelineElement);

        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toTimelineElementInternal(Mockito.any()))
                    .thenReturn(expectedElement);

            Optional<TimelineElementInternal> result = timelineServiceHttp.getTimelineElementForSpecificRecipient(iun, recIndex, category);

            assertTrue(result.isPresent());
            assertEquals(expectedElement, result.get());
        }
    }

    @Test
    void getTimelineStronglyReturnsMappedSetWhenClientReturnsElements() {
        String iun = "iun123";
        boolean confidentialInfoRequired = true;
        TimelineElement timelineElement = new TimelineElement();
        timelineElement.setCategory(TimelineCategory.NOTIFICATION_VIEWED);
        TimelineElementInternal mappedElement = new TimelineElementInternal();

        Mockito.when(timelineClient.getTimeline(Mockito.anyString(), Mockito.anyBoolean(), Mockito.eq(true), Mockito.isNull()))
                .thenReturn(Collections.singletonList(timelineElement));

        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toTimelineElementInternal(Mockito.any()))
                    .thenReturn(mappedElement);

            Set<TimelineElementInternal> result = timelineServiceHttp.getTimelineStrongly(iun, confidentialInfoRequired);

            assertEquals(1, result.size());
            assertTrue(result.contains(mappedElement));
        }
    }

    @Test
    void getTimelineStronglyReturnsEmptySetWhenClientReturnsNull() {
        String iun = "iun123";
        boolean confidentialInfoRequired = true;

        Mockito.when(timelineClient.getTimeline(Mockito.anyString(), Mockito.anyBoolean(), Mockito.eq(true), Mockito.isNull()))
                .thenReturn(null);

        Set<TimelineElementInternal> result = timelineServiceHttp.getTimelineStrongly(iun, confidentialInfoRequired);

        assertTrue(result.isEmpty());
    }

    @Test
    void getTimelineByIunTimelineIdReturnsMappedSet() {
        String iun = "iunTest";
        String timelineId = "timelineIdTest";
        boolean confidentialInfoRequired = true;
        TimelineElement timelineElement = new TimelineElement();
        timelineElement.setCategory(TimelineCategory.NOTIFICATION_VIEWED);
        TimelineElementInternal mappedElement = new TimelineElementInternal();

        Mockito.when(timelineClient.getTimeline(
                iun,
                confidentialInfoRequired,
                false,
                timelineId
        )).thenReturn(Collections.singletonList(timelineElement));

        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toTimelineElementInternal(Mockito.any()))
                    .thenReturn(mappedElement);

            Set<TimelineElementInternal> result = timelineServiceHttp.getTimelineByIunTimelineId(iun, timelineId, confidentialInfoRequired);

            assertEquals(1, result.size());
            assertTrue(result.contains(mappedElement));
        }
    }

    @Test
    void getTimelineByIunTimelineIdReturnsEmptySetWhenClientReturnsNull() {
        String iun = "iunTest";
        String timelineId = "timelineIdTest";
        boolean confidentialInfoRequired = true;

        Mockito.when(timelineClient.getTimeline(
                iun,
                confidentialInfoRequired,
                false,
                timelineId
        )).thenReturn(null);

        Set<TimelineElementInternal> result = timelineServiceHttp.getTimelineByIunTimelineId(iun, timelineId, confidentialInfoRequired);

        assertTrue(result.isEmpty());
    }

    private TimelineElementInternal getTimelineElementInternal() {
        Instant timestamp = Instant.ofEpochMilli(1633072800000L);
        TimelineElementInternal element = new TimelineElementInternal();
        element.setIun("iun123");
        element.setElementId("element123");
        element.setTimestamp(timestamp); // Example timestamp
        element.setPaId("pa123");
        element.setLegalFactsIds(new ArrayList<>());
        element.setCategory(TimelineElementCategoryInt.NOTIFICATION_VIEWED);
        element.setDetails(SendAnalogFeedbackDetailsInt.builder().build());
        element.setStatusInfo(StatusInfoInternal.builder().actual("actual").build());
        element.setNotificationSentAt(timestamp);
        element.setIngestionTimestamp(timestamp);
        element.setEventTimestamp(timestamp);
        return element;
    }
}
