package it.pagopa.pn.deliverypushworkflow.middleware.pnclient.timeline;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.api.TimelineControllerApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.*;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TimelineClientImplTest {
    @Mock
    private TimelineControllerApi timelineControllerApi;

    @InjectMocks
    private TimelineClientImpl timelineServiceClient;

    @Test
    void addTimelineElementReturnsTrueWhenConflictOccurs() {
        NewTimelineElementDto newTimelineElement = Mockito.mock(NewTimelineElementDto.class);
        PnHttpResponseException exception = new PnHttpResponseException("Conflict", HttpStatus.SC_CONFLICT);

        Mockito.doThrow(exception)
                .when(timelineControllerApi)
                .addTimelineElement(newTimelineElement);

        boolean result = timelineServiceClient.addTimelineElement(newTimelineElement);

        assertTrue(result);
    }

    @Test
    void addTimelineElementReturnsFalseWhenOtherErrorOccurs() {
        NewTimelineElementDto newTimelineElement = new NewTimelineElementDto();

        Mockito.doNothing().when(timelineControllerApi).addTimelineElement(Mockito.any());

        boolean result = timelineServiceClient.addTimelineElement(newTimelineElement);

        assertFalse(result);
    }

    @Test
    void addTimelineElement_throwsExceptionOnError() {
        NewTimelineElementDto newTimelineElement = Mockito.mock(NewTimelineElementDto.class);
        PnHttpResponseException exception = new PnHttpResponseException("Errore generico", HttpStatus.SC_INTERNAL_SERVER_ERROR);

        Mockito.doThrow(exception)
                .when(timelineControllerApi)
                .addTimelineElement(newTimelineElement);

        PnHttpResponseException thrown = assertThrows(PnHttpResponseException.class, () ->
                timelineServiceClient.addTimelineElement(newTimelineElement)
        );

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, thrown.getStatusCode());
    }

    @Test
    void retrieveAndIncrementCounterForTimelineEvent_returnsExpectedCounter() {
        String timelineId = "timeline123";
        Long expectedCounter = 42L;

        Mockito.when(timelineControllerApi.retrieveAndIncrementCounterForTimelineEvent(timelineId))
                .thenReturn(expectedCounter);

        Long result = timelineServiceClient.retrieveAndIncrementCounterForTimelineEvent(timelineId);

        assertEquals(expectedCounter, result);
        Mockito.verify(timelineControllerApi).retrieveAndIncrementCounterForTimelineEvent(timelineId);
    }

    @Test
    void getTimelineElement_returnsExpectedElement() {
        String iun = "iun123";
        String timelineId = "timeline123";
        Boolean strongly = true;
        TimelineElementDto expectedElement = new TimelineElementDto();

        Mockito.when(timelineControllerApi.getTimelineElement(iun, timelineId, strongly))
                .thenReturn(expectedElement);

        TimelineElementDto result = timelineServiceClient.getTimelineElement(iun, timelineId, strongly);

        assertEquals(expectedElement, result);
        Mockito.verify(timelineControllerApi).getTimelineElement(iun, timelineId, strongly);
    }

    @Test
    void getTimelineElementDetails_returnsExpectedDetails() {
        String iun = "iun123";
        String timelineId = "timeline123";
        TimelineElementDetailsDto expectedDetails = new TimelineElementDetailsDto();

        Mockito.when(timelineControllerApi.getTimelineElementDetails(iun, timelineId))
                .thenReturn(expectedDetails);

        TimelineElementDetailsDto result = timelineServiceClient.getTimelineElementDetails(iun, timelineId);

        assertEquals(expectedDetails, result);
        Mockito.verify(timelineControllerApi).getTimelineElementDetails(iun, timelineId);
    }

    @Test
    void getTimelineElementDetailForSpecificRecipient_returnsExpectedDetails() {
        String iun = "iun123";
        Integer recIndex = 1;
        Boolean confidentialInfoRequired = true;
        TimelineCategoryDto category = TimelineCategoryDto.NOTIFICATION_VIEWED;
        TimelineElementDetailsDto expectedDetails = new TimelineElementDetailsDto();

        Mockito.when(timelineControllerApi.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, category))
                .thenReturn(expectedDetails);

        TimelineElementDetailsDto result = timelineServiceClient.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, category);

        assertEquals(expectedDetails, result);
        Mockito.verify(timelineControllerApi).getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, category);
    }

    @Test
    void getTimelineElementDetailForSpecificRecipient_throwsException() {
        String iun = "iun123";
        Integer recIndex = 1;
        Boolean confidentialInfoRequired = true;
        TimelineCategoryDto category = TimelineCategoryDto.NOTIFICATION_VIEWED;

        Mockito.when(timelineControllerApi.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, category))
                .thenThrow(new RuntimeException("Errore"));

        assertThrows(RuntimeException.class, () ->
                timelineServiceClient.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, category)
        );
    }

    @Test
    void getTimelineElementForSpecificRecipient_returnsExpectedElement() {
        String iun = "iun123";
        Integer recIndex = 1;
        TimelineCategoryDto category = TimelineCategoryDto.NOTIFICATION_VIEWED;
        TimelineElementDto expectedElement = new TimelineElementDto();

        Mockito.when(timelineControllerApi.getTimelineElementForSpecificRecipient(iun, recIndex, category))
                .thenReturn(expectedElement);

        TimelineElementDto result = timelineServiceClient.getTimelineElementForSpecificRecipient(iun, recIndex, category);

        assertEquals(expectedElement, result);
        Mockito.verify(timelineControllerApi).getTimelineElementForSpecificRecipient(iun, recIndex, category);
    }

    @Test
    void getTimelineElementForSpecificRecipient_throwsException() {
        String iun = "iun123";
        Integer recIndex = 1;
        TimelineCategoryDto category = TimelineCategoryDto.NOTIFICATION_VIEWED;

        Mockito.when(timelineControllerApi.getTimelineElementForSpecificRecipient(iun, recIndex, category))
                .thenThrow(new RuntimeException("Errore"));

        assertThrows(RuntimeException.class, () ->
                timelineServiceClient.getTimelineElementForSpecificRecipient(iun, recIndex, category)
        );
    }

    @Test
    void getTimeline_returnsExpectedList() {
        String iun = "iun123";
        Boolean confidentialInfoRequired = true;
        Boolean strongly = false;
        String timelineId = "timeline123";
        TimelineElementDto expectedElement = new TimelineElementDto();

        Mockito.when(timelineControllerApi.getTimeline(iun, confidentialInfoRequired, strongly, timelineId))
                .thenReturn(List.of(expectedElement));

        List<TimelineElementDto> result = timelineServiceClient.getTimeline(iun, confidentialInfoRequired, strongly, timelineId);

        assertEquals(1, result.size());
        assertEquals(expectedElement, result.get(0));
        Mockito.verify(timelineControllerApi).getTimeline(iun, confidentialInfoRequired, strongly, timelineId);
    }

    @Test
    void getTimeline_throwsException() {
        String iun = "iun123";
        Boolean confidentialInfoRequired = true;
        Boolean strongly = false;
        String timelineId = "timeline123";

        Mockito.when(timelineControllerApi.getTimeline(iun, confidentialInfoRequired, strongly, timelineId))
                .thenThrow(new RuntimeException("Errore"));

        assertThrows(RuntimeException.class, () ->
                timelineServiceClient.getTimeline(iun, confidentialInfoRequired, strongly, timelineId)
        );
    }

    @Test
    void getTimelineAndStatusHistory_returnsExpectedResponse() {
        String iun = "iun123";
        Integer numberOfRecipients = 5;
        Instant createdAt = Instant.now();
        NotificationHistoryResponseDto expectedResponse = new NotificationHistoryResponseDto();

        Mockito.when(timelineControllerApi.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt))
                .thenReturn(expectedResponse);

        NotificationHistoryResponseDto result = timelineServiceClient.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);

        assertEquals(expectedResponse, result);
        Mockito.verify(timelineControllerApi).getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);
    }

    @Test
    void getTimelineAndStatusHistory_throwsException() {
        String iun = "iun123";
        Integer numberOfRecipients = 5;
        Instant createdAt = Instant.now();

        Mockito.when(timelineControllerApi.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt))
                .thenThrow(new RuntimeException("Errore"));

        assertThrows(RuntimeException.class, () ->
                timelineServiceClient.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt)
        );
    }



}
