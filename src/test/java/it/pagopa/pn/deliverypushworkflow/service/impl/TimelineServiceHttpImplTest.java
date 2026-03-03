package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.AddTimelineElementResponse;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.CancellationRequestResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.timeline.TimelineClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

        Mockito.when(timelineClient.addTimelineElement(element, notification)).thenReturn(new AddTimelineElementResponse(null, true));

        AddTimelineElementResponse result = timelineServiceHttp.addTimelineElement(element, notification);

        assertTrue(result.isDuplicate());
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
        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();

        Mockito.when(timelineClient.getTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(timelineElementInternal);

        Optional<TimelineElementInternal> result = timelineServiceHttp.getTimelineElement(iun, timelineId);

        assertTrue(result.isPresent());
    }

    @Test
    void getTimelineElementStronglyReturnsMappedElement() {
        String iun = "iun123";
        String timelineId = "timeline123";
        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();

        Mockito.when(timelineClient.getTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(timelineElementInternal);

        Optional<TimelineElementInternal> result = timelineServiceHttp.getTimelineElementStrongly(iun, timelineId);

        assertTrue(result.isPresent());
    }

    @Test
    void getTimelineReturnsOnlyElementsWithKnownCategory() {
        String iun = "iun123";
        boolean confidentialInfoRequired = true;

        Mockito.when(timelineClient.getTimeline(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any()))
                .thenReturn(List.of(new TimelineElementInternal()));

        Set<TimelineElementInternal> result = timelineServiceHttp.getTimeline(iun, confidentialInfoRequired);

        assertEquals(1, result.size());
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
        TimelineElementDetailsInt timelineElementDetails = Mockito.mock(TimelineElementDetailsInt.class);

        Mockito.when(timelineClient.getTimelineElementDetails(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(timelineElementDetails);

        Optional<TimelineElementDetailsInt> result = timelineServiceHttp.getTimelineElementDetails(iun, timelineId, TimelineElementDetailsInt.class);

        assertTrue(result.isPresent());
    }

    @Test
    void getTimelineElementDetailForSpecificRecipientReturnsMappedDetails() {
        String iun = "iun123";
        int recIndex = 0;
        boolean confidentialInfoRequired = true;
        TimelineElementCategoryInt category = TimelineElementCategoryInt.NOTIFICATION_VIEWED;
        TimelineElementDetailsInt timelineElementDetails = Mockito.mock(TimelineElementDetailsInt.class);


        Mockito.when(timelineClient.getTimelineElementDetailForSpecificRecipient(
                iun,
                recIndex,
                confidentialInfoRequired,
                category
        )).thenReturn(timelineElementDetails);

        Optional<TimelineElementDetailsInt> result = timelineServiceHttp.getTimelineElementDetailForSpecificRecipient(
                iun, recIndex, confidentialInfoRequired, category, TimelineElementDetailsInt.class);

        assertTrue(result.isPresent());
    }

    @Test
    void getTimelineElementForSpecificRecipientReturnsMappedElement() {
        String iun = "iun123";
        int recIndex = 1;
        TimelineElementCategoryInt category = TimelineElementCategoryInt.NOTIFICATION_VIEWED;
        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();

        Mockito.when(timelineClient.getTimelineElementForSpecificRecipient(
                iun,
                recIndex,
                category
        )).thenReturn(timelineElementInternal);

        Optional<TimelineElementInternal> result = timelineServiceHttp.getTimelineElementForSpecificRecipient(iun, recIndex, category);

        assertTrue(result.isPresent());
    }

    @Test
    void getTimelineStronglyReturnsMappedSetWhenClientReturnsElements() {
        String iun = "iun123";
        boolean confidentialInfoRequired = true;
        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();

        Mockito.when(timelineClient.getTimeline(Mockito.anyString(), Mockito.anyBoolean(), Mockito.eq(true), Mockito.isNull()))
                .thenReturn(Collections.singletonList(timelineElementInternal));


        Set<TimelineElementInternal> result = timelineServiceHttp.getTimelineStrongly(iun, confidentialInfoRequired);

        assertEquals(1, result.size());
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
        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();

        Mockito.when(timelineClient.getTimeline(
                iun,
                confidentialInfoRequired,
                false,
                timelineId
        )).thenReturn(Collections.singletonList(timelineElementInternal));

        Set<TimelineElementInternal> result = timelineServiceHttp.getTimelineByIunTimelineId(iun, timelineId, confidentialInfoRequired);

        assertEquals(1, result.size());
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

    @Test
    void getNotificationCancellationRequestedTimestampReturnsTimestampWhenPresent() {
        String iun = "iun123";
        Instant expectedTimestamp = Instant.now();
        CancellationRequestResponse cancellationRequestResponse = new CancellationRequestResponse();
        cancellationRequestResponse.setTimestamp(expectedTimestamp);

        Mockito.when(timelineClient.getNotificationCancellationRequested(iun))
                .thenReturn(Optional.of(cancellationRequestResponse));

        Optional<Instant> result = timelineServiceHttp.getNotificationCancellationRequestedTimestamp(iun);

        assertTrue(result.isPresent());
        assertEquals(expectedTimestamp, result.get());
    }

    @Test
    void getNotificationCancellationRequestedTimestampReturnsEmptyWhenNotPresent() {
        String iun = "iun123";

        Mockito.when(timelineClient.getNotificationCancellationRequested(iun))
                .thenReturn(Optional.empty());

        Optional<Instant> result = timelineServiceHttp.getNotificationCancellationRequestedTimestamp(iun);

        assertFalse(result.isPresent());
    }

    @Test
    void getNotificationCancellationRequestedTimestampReturnsEmptyWhenTimestampIsNull() {
        String iun = "iun123";
        CancellationRequestResponse cancellationRequestResponse = new CancellationRequestResponse();
        cancellationRequestResponse.setTimestamp(null);

        Mockito.when(timelineClient.getNotificationCancellationRequested(iun))
                .thenReturn(Optional.of(cancellationRequestResponse));

        Optional<Instant> result = timelineServiceHttp.getNotificationCancellationRequestedTimestamp(iun);

        assertFalse(result.isPresent());
    }

    @Test
    void isNotificationCancellationRequestedReturnsTrueWhenPresent() {
        String iun = "iun123";
        Instant timestamp = Instant.now();
        CancellationRequestResponse cancellationRequestResponse = new CancellationRequestResponse();
        cancellationRequestResponse.setTimestamp(timestamp);

        Mockito.when(timelineClient.getNotificationCancellationRequested(iun))
                .thenReturn(Optional.of(cancellationRequestResponse));

        boolean result = timelineServiceHttp.isNotificationCancellationRequested(iun);

        assertTrue(result);
    }

    @Test
    void isNotificationCancellationRequestedReturnsFalseWhenNotPresent() {
        String iun = "iun123";

        Mockito.when(timelineClient.getNotificationCancellationRequested(iun))
                .thenReturn(Optional.empty());

        boolean result = timelineServiceHttp.isNotificationCancellationRequested(iun);

        assertFalse(result);
    }

    @Test
    void isNotificationCancellationRequestedReturnsFalseWhenTimestampIsNull() {
        String iun = "iun123";
        CancellationRequestResponse cancellationRequestResponse = new CancellationRequestResponse();
        cancellationRequestResponse.setTimestamp(null);

        Mockito.when(timelineClient.getNotificationCancellationRequested(iun))
                .thenReturn(Optional.of(cancellationRequestResponse));

        boolean result = timelineServiceHttp.isNotificationCancellationRequested(iun);

        assertFalse(result);
    }

}
