package it.pagopa.pn.deliverypushworkflow.action.analogworkflow;

import it.pagopa.pn.deliverypushworkflow.action.details.AnalogWorkflowTimeoutDetails;
import it.pagopa.pn.deliverypushworkflow.action.utils.AnalogDeliveryTimeoutUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.PaperChannelUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypushworkflow.service.*;
import it.pagopa.pn.deliverypushworkflow.utils.FeatureEnabledUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AnalogWorkflowTimeoutActionHandlerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private FeatureEnabledUtils featureEnabledUtils;
    @Mock
    private PaperTrackerService paperTrackerService;
    @Mock
    private SaveLegalFactsService saveLegalFactsService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private DocumentCreationRequestService documentCreationRequestService;
    @Mock
    private AnalogDeliveryTimeoutUtils analogDeliveryTimeoutUtils;
    @Mock
    private PaperChannelUtils paperChannelUtils;

    @InjectMocks
    private AnalogWorkflowTimeoutActionHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new AnalogWorkflowTimeoutActionHandler(
                notificationService,
                timelineService,
                featureEnabledUtils,
                paperTrackerService,
                saveLegalFactsService,
                timelineUtils,
                documentCreationRequestService,
                analogDeliveryTimeoutUtils,
                paperChannelUtils
        );
    }

    @Test
    void shouldNotHandleWhenFeatureDisabled() {
        String iun = "IUN1";
        NotificationInt notification = mock(NotificationInt.class);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(notification.getSentAt()).thenReturn(Instant.now());
        when(featureEnabledUtils.isAnalogWorkflowTimeoutFeatureEnabled(any())).thenReturn(false);

        handler.handleAnalogWorkflowTimeout(iun, "timelineId", 0, mock(AnalogWorkflowTimeoutDetails.class), Instant.now());

        verifyNoInteractions(timelineService, paperTrackerService, saveLegalFactsService, timelineUtils, documentCreationRequestService);
    }

    @Test
    void shouldNotHandleWhenTimelineElementNotPresent() {
        String iun = "IUN2";
        NotificationInt notification = mock(NotificationInt.class);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(notification.getSentAt()).thenReturn(Instant.now());
        when(featureEnabledUtils.isAnalogWorkflowTimeoutFeatureEnabled(any())).thenReturn(true);
        when(timelineService.getTimelineElementDetails(eq(iun), any(), eq(SendAnalogDetailsInt.class))).thenReturn(Optional.empty());

        handler.handleAnalogWorkflowTimeout(iun, "timelineId", 0, mock(AnalogWorkflowTimeoutDetails.class), Instant.now());

        verifyNoMoreInteractions(paperTrackerService, saveLegalFactsService, timelineUtils, documentCreationRequestService);
    }

    @Test
    void shouldSkipWhenDematPresent() {
        String iun = "IUN3";
        int recIndex = 0;
        int sentAttemptMade = 0;

        AnalogWorkflowTimeoutDetails details = mock(AnalogWorkflowTimeoutDetails.class);
        when(details.getSentAttemptMade()).thenReturn(sentAttemptMade);

        NotificationInt notification = mock(NotificationInt.class);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(notification.getSentAt()).thenReturn(Instant.now());
        when(featureEnabledUtils.isAnalogWorkflowTimeoutFeatureEnabled(any())).thenReturn(true);

        String prepareRequestId = "PREP_REQ_ID";
        when(paperChannelUtils.buildPrepareAnalogDomicileEventId(notification, recIndex, sentAttemptMade)).thenReturn(prepareRequestId);
        when(paperTrackerService.isPresentDematForPrepareRequest(prepareRequestId)).thenReturn(true);
        SendAnalogDetailsInt sendAnalogDetails = mock(SendAnalogDetailsInt.class);
        when(timelineService.getTimelineElementDetails(eq(iun), any(), eq(SendAnalogDetailsInt.class)))
                .thenReturn(Optional.of(sendAnalogDetails));
        when(paperTrackerService.isPresentDematForPrepareRequest("PREP_REQ_ID")).thenReturn(true);

        handler.handleAnalogWorkflowTimeout(iun, "timelineId", recIndex, mock(AnalogWorkflowTimeoutDetails.class), Instant.now());

        verifyNoInteractions(saveLegalFactsService, timelineUtils, documentCreationRequestService);
    }

    @Test
    void shouldSkipWhenDematNotPresent_SendAnalogFeedbackPresent() {
        String iun = "IUN3";
        NotificationInt notification = mock(NotificationInt.class);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(notification.getSentAt()).thenReturn(Instant.now());
        when(featureEnabledUtils.isAnalogWorkflowTimeoutFeatureEnabled(any())).thenReturn(true);

        SendAnalogDetailsInt sendAnalogDetails = mock(SendAnalogDetailsInt.class);
        when(sendAnalogDetails.getPrepareRequestId()).thenReturn("PREP_REQ_ID");
        when(timelineService.getTimelineElementDetails(eq(iun), any(), eq(SendAnalogDetailsInt.class)))
                .thenReturn(Optional.of(sendAnalogDetails));
        when(paperTrackerService.isPresentDematForPrepareRequest("PREP_REQ_ID")).thenReturn(false);
        when(analogDeliveryTimeoutUtils.isSendAnalogFeedbackPresentInTimeline(anyString(), anyInt(), anyInt()))
                .thenReturn(true);

        handler.handleAnalogWorkflowTimeout(iun, "timelineId", 0, mock(AnalogWorkflowTimeoutDetails.class), Instant.now());

        verifyNoInteractions(saveLegalFactsService, timelineUtils, documentCreationRequestService);
    }

    @Test
    void shouldHandleAndCreateLegalFactAndTimeline() {
        String iun = "IUN4";
        int recIndex = 0;
        int sentAttemptMade = 2;
        String legalFactId = "LEGAL_FACT_ID";
        String prepareRequestId = "PREP_REQ_ID";
        String sendAnalogDomicileTimelineId = "SEND_ANALOG_DOMICILE_TIMELINE_ID";
        Instant timeoutDate = Instant.now();

        NotificationInt notification = mock(NotificationInt.class);
        NotificationRecipientInt recipient = mock(NotificationRecipientInt.class);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(notification.getSentAt()).thenReturn(Instant.now());
        when(notification.getRecipients()).thenReturn(Collections.singletonList(recipient));
        when(featureEnabledUtils.isAnalogWorkflowTimeoutFeatureEnabled(any())).thenReturn(true);

        SendAnalogDetailsInt sendAnalogDetails = mock(SendAnalogDetailsInt.class);
        when(sendAnalogDetails.getPhysicalAddress()).thenReturn(getPhysicalAddress());
        when(timelineService.getTimelineElementDetails(eq(iun), any(), eq(SendAnalogDetailsInt.class)))
                .thenReturn(Optional.of(sendAnalogDetails));
        when(paperChannelUtils.buildPrepareAnalogDomicileEventId(notification, recIndex, sentAttemptMade)).thenReturn(prepareRequestId);
        when(paperTrackerService.isPresentDematForPrepareRequest(prepareRequestId)).thenReturn(false);

        AnalogWorkflowTimeoutDetails details = mock(AnalogWorkflowTimeoutDetails.class);
        when(details.getSentAttemptMade()).thenReturn(sentAttemptMade);

        when(saveLegalFactsService.sendCreationRequestForAnalogDeliveryWorkflowTimeoutLegalFact(
                eq(notification), eq(recipient), eq(getPhysicalAddress()), eq(String.valueOf(sentAttemptMade)), eq(timeoutDate)))
                .thenReturn(legalFactId);
        TimelineElementInternal timelineElement = mock(TimelineElementInternal.class);
        when(timelineUtils.buildSendAnalogTimeoutCreationRequest(
                notification, recIndex, timeoutDate, sentAttemptMade, sendAnalogDomicileTimelineId, legalFactId))
                .thenReturn(timelineElement);
        when(timelineElement.getElementId()).thenReturn("TIMELINE_ID");

        handler.handleAnalogWorkflowTimeout(iun, sendAnalogDomicileTimelineId, recIndex, details, timeoutDate);

        verify(saveLegalFactsService).sendCreationRequestForAnalogDeliveryWorkflowTimeoutLegalFact(
                eq(notification), eq(recipient), eq(getPhysicalAddress()), eq(String.valueOf(sentAttemptMade)), eq(timeoutDate));
        verify(timelineService, times(1)).addTimelineElement(timelineElement, notification);
        verify(documentCreationRequestService).addDocumentCreationRequest(
                eq(legalFactId), eq(iun), eq(recIndex), eq(DocumentCreationTypeInt.ANALOG_DELIVERY_TIMEOUT), eq("TIMELINE_ID"));
    }

    private PhysicalAddressInt getPhysicalAddress() {
        PhysicalAddressInt address = new PhysicalAddressInt();
        address.setAddress("Via Roma 1");
        address.setAt("Roma");
        address.setZip("00100");
        return address;
    }
}

