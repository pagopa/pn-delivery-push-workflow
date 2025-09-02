package it.pagopa.pn.deliverypushworkflow.action.analogworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.deliverypushworkflow.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypushworkflow.action.utils.AnalogDeliveryTimeoutUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogTimeoutCreationRequestDetailsInt;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;

class AnalogWorkflowDeliveryTimeoutHandlerTest {

    @Mock
    TimelineService timelineService;
    @Mock
    TimelineUtils timelineUtils;
    @Mock
    NotificationService notificationService;
    @Mock
    AnalogWorkflowHandler analogWorkflowHandler;
    @Mock
    AnalogDeliveryTimeoutUtils analogDeliveryTimeoutUtils;

    @InjectMocks AnalogWorkflowDeliveryTimeoutHandler handler;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        handler = new AnalogWorkflowDeliveryTimeoutHandler(
                timelineService, timelineUtils, notificationService, analogWorkflowHandler, analogDeliveryTimeoutUtils
        );
    }

    @Test
    void handleDeliveryTimeout_firstAttempt_notificationNotViewed() {
        String iun = "testIun";
        int recIndex = 0;
        String sendAnalogTimeoutCreationRequestId = "sendAnalogTimeoutCreationRequestId";
        DocumentCreationResponseActionDetails actionDetails = mock(DocumentCreationResponseActionDetails.class);
        when(actionDetails.getKey()).thenReturn("key");
        when(actionDetails.getTimelineId()).thenReturn(sendAnalogTimeoutCreationRequestId);

        NotificationInt notification = mock(NotificationInt.class);
        when(notification.getIun()).thenReturn(iun);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);

        SendAnalogTimeoutCreationRequestDetailsInt details = mock(SendAnalogTimeoutCreationRequestDetailsInt.class);
        when(details.getSentAttemptMade()).thenReturn(0);
        String sendAnalogDomicileCreationRequestId = "sendAnalogDomicileCreationRequestId";
        when(details.getRelatedRequestId()).thenReturn(sendAnalogDomicileCreationRequestId);
        when(details.getTimeoutDate()).thenReturn(Instant.now());
        when(details.getLegalFactId()).thenReturn("legalFactId");
        when(timelineService.getTimelineElementDetails(eq(iun), eq(sendAnalogTimeoutCreationRequestId), eq(SendAnalogTimeoutCreationRequestDetailsInt.class)))
                .thenReturn(Optional.of(details));

        SendAnalogDetailsInt sendAnalogDetails = mock(SendAnalogDetailsInt.class);
        when(timelineService.getTimelineElementDetails(eq(iun), eq(sendAnalogDomicileCreationRequestId), eq(SendAnalogDetailsInt.class)))
                .thenReturn(Optional.of(sendAnalogDetails));
        TimelineElementInternal timelineElement = mock(TimelineElementInternal.class);
        when(timelineUtils.buildSendAnalogTimeout(any(), any(), any(), any(), any())).thenReturn(timelineElement);

        when(timelineUtils.checkIsNotificationViewed(iun, recIndex)).thenReturn(false);

        handler.handleDeliveryTimeout(iun, recIndex, actionDetails);

        verify(analogWorkflowHandler).nextWorkflowStep(notification, recIndex, 1, null);
    }

    @Test
    void handleDeliveryTimeout_firstAttempt_notificationViewed() {
        String iun = "testIun";
        int recIndex = 0;
        String sendAnalogTimeoutCreationRequestId = "sendAnalogTimeoutCreationRequestId";
        DocumentCreationResponseActionDetails actionDetails = mock(DocumentCreationResponseActionDetails.class);
        when(actionDetails.getKey()).thenReturn("key");
        when(actionDetails.getTimelineId()).thenReturn(sendAnalogTimeoutCreationRequestId);

        NotificationInt notification = mock(NotificationInt.class);
        when(notification.getIun()).thenReturn(iun);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);

        SendAnalogTimeoutCreationRequestDetailsInt details = mock(SendAnalogTimeoutCreationRequestDetailsInt.class);
        when(details.getSentAttemptMade()).thenReturn(0);
        String sendAnalogDomicileCreationRequestId = "sendAnalogDomicileCreationRequestId";
        when(details.getRelatedRequestId()).thenReturn(sendAnalogDomicileCreationRequestId);
        when(details.getTimeoutDate()).thenReturn(Instant.now());
        when(details.getLegalFactId()).thenReturn("legalFactId");
        when(timelineService.getTimelineElementDetails(eq(iun), eq(sendAnalogTimeoutCreationRequestId), eq(SendAnalogTimeoutCreationRequestDetailsInt.class)))
                .thenReturn(Optional.of(details));

        SendAnalogDetailsInt sendAnalogDetails = mock(SendAnalogDetailsInt.class);
        when(timelineService.getTimelineElementDetails(eq(iun), eq(sendAnalogDomicileCreationRequestId), eq(SendAnalogDetailsInt.class)))
                .thenReturn(Optional.of(sendAnalogDetails));
        TimelineElementInternal timelineElement = mock(TimelineElementInternal.class);
        when(timelineUtils.buildSendAnalogTimeout(any(), any(), any(), any(), any())).thenReturn(timelineElement);

        when(timelineUtils.checkIsNotificationViewed(iun, recIndex)).thenReturn(true);

        handler.handleDeliveryTimeout(iun, recIndex, actionDetails);

        verify(analogWorkflowHandler, never()).nextWorkflowStep(notification, recIndex, 0, null);
    }

    @Test
    void testHandleDeliveryTimeout_SecondAttempt() {
        String iun = "iun";
        int recIndex = 0;
        String sendAnalogTimeoutCreationRequestId = "sendAnalogTimeoutCreationRequestId";
        String key = "key";
        DocumentCreationResponseActionDetails actionDetails = mock(DocumentCreationResponseActionDetails.class);
        NotificationInt notification = mock(NotificationInt.class);
        SendAnalogDetailsInt sendAnalogDetails = mock(SendAnalogDetailsInt.class);

        when(actionDetails.getKey()).thenReturn(key);
        when(actionDetails.getTimelineId()).thenReturn(sendAnalogTimeoutCreationRequestId);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(notification.getIun()).thenReturn(iun);

        SendAnalogTimeoutCreationRequestDetailsInt timeoutDetails = mock(SendAnalogTimeoutCreationRequestDetailsInt.class);
        when(timeoutDetails.getSentAttemptMade()).thenReturn(1);
        when(timeoutDetails.getTimeoutDate()).thenReturn(Instant.now());
        String sendAnalogDomicileCreationRequestId = "sendAnalogDomicileCreationRequestId";
        when(timeoutDetails.getRelatedRequestId()).thenReturn(sendAnalogDomicileCreationRequestId);
        when(timelineService.getTimelineElementDetails(eq(iun), eq(sendAnalogTimeoutCreationRequestId), eq(SendAnalogTimeoutCreationRequestDetailsInt.class)))
                .thenReturn(Optional.of(timeoutDetails));

        when(timelineService.getTimelineElementDetails(eq(iun), eq(sendAnalogDomicileCreationRequestId), eq(SendAnalogDetailsInt.class)))
                .thenReturn(Optional.of(sendAnalogDetails));

        handler.handleDeliveryTimeout(iun, recIndex, actionDetails);

        verify(analogDeliveryTimeoutUtils).buildAnalogFailureWorkflowTimeoutElement(eq(notification), eq(recIndex), any());
    }

    @Test
    void testHandleSecondAttemptThrowsException() {
        String iun = "iun";
        int recIndex = 0;
        String sendAnalogTimeoutCreationRequestId = "sendAnalogTimeoutCreationRequestId";

        DocumentCreationResponseActionDetails actionDetails = mock(DocumentCreationResponseActionDetails.class);
        when(actionDetails.getTimelineId()).thenReturn(sendAnalogTimeoutCreationRequestId);

        NotificationInt notification = mock(NotificationInt.class);
        when(notification.getIun()).thenReturn(iun);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);

        SendAnalogTimeoutCreationRequestDetailsInt sendAnalogTimeoutCreationRequestDetails = mock(SendAnalogTimeoutCreationRequestDetailsInt.class);
        when(sendAnalogTimeoutCreationRequestDetails.getSentAttemptMade()).thenReturn(1);
        Instant timeoutDate = Instant.now();
        when(sendAnalogTimeoutCreationRequestDetails.getTimeoutDate()).thenReturn(timeoutDate);
        when(timelineService.getTimelineElementDetails(eq(iun), eq(sendAnalogTimeoutCreationRequestId), eq(SendAnalogTimeoutCreationRequestDetailsInt.class)))
                .thenReturn(Optional.of(sendAnalogTimeoutCreationRequestDetails));

        PnAuditLogEvent auditLogEvent = mock(PnAuditLogEvent.class);

        PnInternalException ex = new PnInternalException("Simulated exception", "test");
        doThrow(ex)
                .when(analogDeliveryTimeoutUtils).buildAnalogFailureWorkflowTimeoutElement(notification, recIndex, timeoutDate);
        when(auditLogEvent.generateFailure(anyString(), any())).thenReturn(auditLogEvent);

        Assertions.assertThrows(PnInternalException.class, () ->
                handler.handleDeliveryTimeout(iun, recIndex, actionDetails)
        );
    }

    @Test
    void handleDeliveryTimeout_timelineDetailsNotFound_throwsException() {
        String iun = "testIun";
        int recIndex = 0;
        String timelineId = "timelineId";
        DocumentCreationResponseActionDetails actionDetails = mock(DocumentCreationResponseActionDetails.class);
        when(actionDetails.getKey()).thenReturn("key");
        when(actionDetails.getTimelineId()).thenReturn(timelineId);

        NotificationInt notification = mock(NotificationInt.class);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);

        when(timelineService.getTimelineElementDetails(eq(iun), eq(timelineId), eq(SendAnalogTimeoutCreationRequestDetailsInt.class)))
                .thenThrow(new RuntimeException("Timeline not found"));

        Assertions.assertThrows(PnInternalException.class, () -> handler.handleDeliveryTimeout(iun, recIndex, actionDetails));
    }

    @Test
    void testBuildSendAnalogTimeoutElement_sendAnalogDetailsNotFound() {
        String iun = "iun";
        int recIndex = 0;
        String sendAnalogTimeoutCreationRequestId = "sendAnalogTimeoutCreationRequestId";
        String key = "key";
        DocumentCreationResponseActionDetails actionDetails = mock(DocumentCreationResponseActionDetails.class);
        when(actionDetails.getKey()).thenReturn(key);
        when(actionDetails.getTimelineId()).thenReturn(sendAnalogTimeoutCreationRequestId);

        NotificationInt notification = mock(NotificationInt.class);
        when(notification.getIun()).thenReturn(iun);



        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        SendAnalogTimeoutCreationRequestDetailsInt details = mock(SendAnalogTimeoutCreationRequestDetailsInt.class);
        when(details.getSentAttemptMade()).thenReturn(0);
        when(details.getTimeoutDate()).thenReturn(Instant.now());
        String sendAnalogDomicileCreationRequestId = "sendAnalogDomicileCreationRequestId";
        when(details.getRelatedRequestId()).thenReturn(sendAnalogDomicileCreationRequestId);
        when(timelineService.getTimelineElementDetails(eq(iun), eq(sendAnalogTimeoutCreationRequestId), eq(SendAnalogTimeoutCreationRequestDetailsInt.class)))
                .thenReturn(Optional.of(details));
        when(timelineService.getTimelineElementDetails(eq(iun), eq(sendAnalogDomicileCreationRequestId), eq(SendAnalogDetailsInt.class)))
                .thenReturn(Optional.empty());

        when(timelineUtils.checkIsNotificationViewed(iun, recIndex)).thenReturn(true);

        handler.handleDeliveryTimeout(iun, recIndex, actionDetails);

        verify(timelineService, never()).addTimelineElement(any(), any());
    }

}
