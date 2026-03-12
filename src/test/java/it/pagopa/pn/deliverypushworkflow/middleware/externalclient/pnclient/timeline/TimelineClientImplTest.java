package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.timeline;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.AddTimelineElementResponse;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.api.TimelineControllerApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.*;
import it.pagopa.pn.deliverypushworkflow.service.mapper.TimelineServiceMapper;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimelineClientImplTest {
    @Mock
    private TimelineControllerApi timelineControllerApi;

    @Mock
    private TimelineServiceMapper timelineServiceMapper;

    @InjectMocks
    private TimelineClientImpl timelineServiceClient;

    @Test
    void addTimelineElementReturnsTrueWhenConflictOccurs() {
        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        when(timelineElementInternal.getElementId()).thenReturn("elementId");
        NotificationInt notificationInt = Mockito.mock(NotificationInt.class);

        NewTimelineElement newTimelineElement = Mockito.mock(NewTimelineElement.class);
        when(timelineServiceMapper.getNewTimelineElement(timelineElementInternal, notificationInt))
                .thenReturn(newTimelineElement);

        PnHttpResponseException exception = new PnHttpResponseException("Conflict", HttpStatus.SC_CONFLICT);

        Mockito.doThrow(exception)
                .when(timelineControllerApi)
                .addTimelineElement(newTimelineElement);

        AddTimelineElementResponse result = timelineServiceClient.addTimelineElement(timelineElementInternal, notificationInt);
        assertEquals(timelineElementInternal.getElementId(), result.getTimelineElementId());
        assertTrue(result.isDuplicate());
    }

    @Test
    void addTimelineElementReturnsFalseWhenOtherErrorOccurs() {
        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        NotificationInt notificationInt = Mockito.mock(NotificationInt.class);

        NewTimelineElement newTimelineElement = new NewTimelineElement();
        when(timelineServiceMapper.getNewTimelineElement(timelineElementInternal, notificationInt))
                .thenReturn(newTimelineElement);

        when(timelineControllerApi.addTimelineElement(Mockito.any()))
                .thenThrow(new RuntimeException("error generic"));

        Assertions.assertThrows(RuntimeException.class, () -> timelineServiceClient.addTimelineElement(timelineElementInternal, notificationInt));

    }

    @Test
    void addTimelineElement_throwsExceptionOnError() {
        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        NotificationInt notificationInt = Mockito.mock(NotificationInt.class);
        NewTimelineElement newTimelineElement = Mockito.mock(NewTimelineElement.class);
        PnHttpResponseException exception = new PnHttpResponseException("Errore generico", HttpStatus.SC_INTERNAL_SERVER_ERROR);

        when(timelineServiceMapper.getNewTimelineElement(timelineElementInternal, notificationInt))
                .thenReturn(newTimelineElement);

        Mockito.doThrow(exception)
                .when(timelineControllerApi)
                .addTimelineElement(newTimelineElement);

        PnHttpResponseException thrown = assertThrows(PnHttpResponseException.class, () ->
                timelineServiceClient.addTimelineElement(timelineElementInternal, notificationInt)
        );

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, thrown.getStatusCode());
    }

    @Test
    void retrieveAndIncrementCounterForTimelineEvent_returnsExpectedCounter() {
        String timelineId = "timeline123";
        Long expectedCounter = 42L;

        when(timelineControllerApi.retrieveAndIncrementCounterForTimelineEvent(timelineId))
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
        TimelineElementInternal expectedElement = Mockito.mock(TimelineElementInternal.class);
        TimelineElement timelineElement = new TimelineElement();

        when(timelineControllerApi.getTimelineElement(iun, timelineId, strongly))
                .thenReturn(timelineElement);

        when(timelineServiceMapper.toTimelineElementInternal(timelineElement)).thenReturn(expectedElement);

        TimelineElementInternal result = timelineServiceClient.getTimelineElement(iun, timelineId, strongly);

        assertEquals(expectedElement, result);
        Mockito.verify(timelineControllerApi).getTimelineElement(iun, timelineId, strongly);
    }

    @Test
    void getTimelineElementDetails_returnsExpectedDetails() {
        String iun = "iun123";
        String timelineId = "timeline123";
        TimelineElementDetails timelineElementDetails = new SendDigitalDetails().categoryType("SEND_DIGITAL_DOMICILE");
        TimelineElementDetailsInt expectedDetails = Mockito.mock(TimelineElementDetailsInt.class);

        when(timelineControllerApi.getTimelineElementDetails(iun, timelineId))
                .thenReturn(timelineElementDetails);

        when(timelineServiceMapper.toTimelineElementDetailsInt(timelineElementDetails, TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE))
                        .thenReturn(expectedDetails);

        TimelineElementDetailsInt result = timelineServiceClient.getTimelineElementDetails(iun, timelineId);

        assertEquals(expectedDetails, result);
        Mockito.verify(timelineControllerApi).getTimelineElementDetails(iun, timelineId);
    }

    @Test
    void getTimelineElementDetails_handlesNullDetails() {
        String iun = "iun123";
        String timelineId = "timeline123";

        when(timelineControllerApi.getTimelineElementDetails(iun, timelineId))
                .thenReturn(null);

        TimelineElementDetailsInt result = timelineServiceClient.getTimelineElementDetails(iun, timelineId);

        assertNull(result);
        Mockito.verify(timelineControllerApi).getTimelineElementDetails(iun, timelineId);
        Mockito.verify(timelineServiceMapper, never()).toTimelineElementDetailsInt(Mockito.any(), Mockito.any());
    }

    @Test
    void getTimelineElementDetailForSpecificRecipient_returnsExpectedDetails() {
        String iun = "iun123";
        Integer recIndex = 1;
        Boolean confidentialInfoRequired = true;
        TimelineCategory category = TimelineCategory.NOTIFICATION_VIEWED;
        TimelineElementCategoryInt categoryInt = TimelineElementCategoryInt.NOTIFICATION_VIEWED;
        TimelineElementDetails timelineElementDetails = new NotificationViewedDetails().categoryType("NOTIFICATION_VIEWED");
        TimelineElementDetailsInt expectedDetails = Mockito.mock(TimelineElementDetailsInt.class);

        when(timelineControllerApi.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, category))
                .thenReturn(timelineElementDetails);

        when(timelineServiceMapper.toTimelineElementDetailsInt(timelineElementDetails, TimelineElementCategoryInt.NOTIFICATION_VIEWED))
                .thenReturn(expectedDetails);

        TimelineElementDetailsInt result = timelineServiceClient.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, categoryInt);

        assertEquals(expectedDetails, result);
        Mockito.verify(timelineControllerApi).getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, category);
    }

    @Test
    void getTimelineElementDetailForSpecificRecipient_throwsException() {
        String iun = "iun123";
        Integer recIndex = 1;
        Boolean confidentialInfoRequired = true;
        TimelineCategory category = TimelineCategory.NOTIFICATION_VIEWED;
        TimelineElementCategoryInt categoryInt = TimelineElementCategoryInt.NOTIFICATION_VIEWED;

        when(timelineControllerApi.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, category))
                .thenThrow(new RuntimeException("Errore"));

        assertThrows(RuntimeException.class, () ->
                timelineServiceClient.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, categoryInt)
        );
    }

    @Test
    void getTimelineElementForSpecificRecipient_returnsExpectedElement() {
        String iun = "iun123";
        Integer recIndex = 1;
        TimelineCategory category = TimelineCategory.NOTIFICATION_VIEWED;
        TimelineElementCategoryInt categoryInt = TimelineElementCategoryInt.NOTIFICATION_VIEWED;
        TimelineElement timelineElement = new TimelineElement();
        TimelineElementInternal expectedElement = Mockito.mock(TimelineElementInternal.class);

        when(timelineControllerApi.getTimelineElementForSpecificRecipient(iun, recIndex, category))
                .thenReturn(timelineElement);

        when(timelineServiceMapper.toTimelineElementInternal(timelineElement)).thenReturn(expectedElement);

        TimelineElementInternal result = timelineServiceClient.getTimelineElementForSpecificRecipient(iun, recIndex, categoryInt);

        assertEquals(expectedElement, result);
        Mockito.verify(timelineControllerApi).getTimelineElementForSpecificRecipient(iun, recIndex, category);
    }

    @Test
    void getTimelineElementForSpecificRecipient_throwsException() {
        String iun = "iun123";
        Integer recIndex = 1;
        TimelineCategory category = TimelineCategory.NOTIFICATION_VIEWED;
        TimelineElementCategoryInt categoryInt = TimelineElementCategoryInt.NOTIFICATION_VIEWED;

        when(timelineControllerApi.getTimelineElementForSpecificRecipient(iun, recIndex, category))
                .thenThrow(new RuntimeException("Errore"));

        assertThrows(RuntimeException.class, () ->
                timelineServiceClient.getTimelineElementForSpecificRecipient(iun, recIndex, categoryInt)
        );
    }

    @Test
    void getTimeline_returnsExpectedList() {
        String iun = "iun123";
        Boolean confidentialInfoRequired = true;
        Boolean strongly = false;
        String timelineId = "timeline123";
        TimelineElement timelineElementKnown = new TimelineElement().category(TimelineCategory.NOTIFICATION_VIEWED);
        TimelineElement timelineElementUnknown = new TimelineElement().category(TimelineCategory.NORMALIZED_ADDRESS);
        TimelineElementInternal expectedElement = new TimelineElementInternal();

        when(timelineControllerApi.getTimeline(iun, confidentialInfoRequired, strongly, timelineId))
                .thenReturn(List.of(timelineElementKnown, timelineElementUnknown));

        when(timelineServiceMapper.toTimelineElementInternal(timelineElementKnown)).thenReturn(expectedElement);

        List<TimelineElementInternal> result = timelineServiceClient.getTimeline(iun, confidentialInfoRequired, strongly, timelineId);

        assertEquals(1, result.size()); // Only one known category should be returned
        assertEquals(expectedElement, result.getFirst());
        Mockito.verify(timelineControllerApi).getTimeline(iun, confidentialInfoRequired, strongly, timelineId);
    }

    @Test
    void getTimeline_throwsException() {
        String iun = "iun123";
        Boolean confidentialInfoRequired = true;
        Boolean strongly = false;
        String timelineId = "timeline123";

        when(timelineControllerApi.getTimeline(iun, confidentialInfoRequired, strongly, timelineId))
                .thenThrow(new RuntimeException("Errore"));

        assertThrows(RuntimeException.class, () ->
                timelineServiceClient.getTimeline(iun, confidentialInfoRequired, strongly, timelineId)
        );
    }

}
