package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkValidationDetails;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkError;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkErrorCause;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.NotificationViewedCreationRequestDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.ScheduleRefinementDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.actionmanager.api.ActionApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.api.CheckAddressApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.model.CheckAddressResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationStatus;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventAction;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventPool;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.SafeStorageService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class ReworkValidationHandlerTest {

    @Mock
    private CheckAddressApi checkAddressApi;
    @Mock
    private ActionApi actionManagerApi;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private ReworkRequestEventPool reworkRequestEventPool;
    @Mock
    private PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private SafeStorageService safeStorageService;

    private ReworkValidationHandler notificationReworkHandler;

    @BeforeEach
    void setup() {
        notificationReworkHandler = new ReworkValidationHandler(checkAddressApi, actionManagerApi, notificationService, timelineService, timelineUtils, reworkRequestEventPool, pnDeliveryPushWorkflowConfigs, safeStorageService);
    }

    @Test
    void handleNotificationRework_OK() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        NotificationDocumentInt doc = NotificationDocumentInt.builder()
                .ref(NotificationDocumentInt.Ref.builder().key("key").build())
                .build();

        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .documents(List.of(doc))
                .build();

        Set<TimelineElementInternal> timeline = new HashSet<>();
        TimelineElementInternal timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE);
        timelineElement.setElementId("PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
        timelineElement.setElementId("SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.REFINEMENT);
        timelineElement.setElementId("REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);


        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.EFFECTIVE_DATE);

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);
        when(pnDeliveryPushWorkflowConfigs.getReworkTTLAddressRange()).thenReturn(10);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(),anyInt(),any())).thenReturn(notificationHistoryResponse);

        FileDownloadResponse fileResponse = new FileDownloadResponse();
        fileResponse.setRetentionUntil(OffsetDateTime.now().plusDays(120));

        when(safeStorageService.getFile(any(),any(),any())).thenReturn(Mono.just(fileResponse));

        CheckAddressResponse response = new CheckAddressResponse();
        response.setFound(true);
        response.setEndValidity(Instant.now().plus(20, java.time.temporal.ChronoUnit.DAYS));
        when(checkAddressApi.checkAddress(anyString())).thenReturn(response);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, times(1)).insertAction(any());
        verify(reworkRequestEventPool, never()).scheduleFutureAction(any(), any());
    }

    @Test
    void handleNotificationCancelled() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        ArrayList<NotificationReworkError> errorList = new ArrayList<>();
        errorList.add(NotificationReworkError.builder().cause("NOTIFICATION_CANCELLED").description("La notifica è stata cancellata").build());

        ReworkRequestEventAction reworkRequest = new ReworkRequestEventAction();
        reworkRequest.setError(errorList);
        reworkRequest.setIun(action.getIun());
        reworkRequest.setReworkId(detail.getReworkId());
        reworkRequest.setOperation("ERROR");

        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(true);

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.NOTIFICATION_CANCELLED.getCause(), capturedErrorList.getFirst().getCause());
    }

    @Test
    void handleCheckNotificationStatusKo() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("0");
        detail.setReworkRecIndex("RECINDEX_1");
        detail.setReworkPcRetry("PCRETRY_0");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .build();

        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.INVALID_RECINDEX.getCause(), capturedErrorList.getFirst().getCause());
    }

    @Test
    void handleCheckNotificationStatusKo_MONO() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("ATTEMPT_0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .build();

        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);

        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.DELIVERED);
        when(timelineService.getTimelineAndStatusHistory(any(),anyInt(),any())).thenReturn(notificationHistoryResponse);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.INVALID_NOTIFICATION_STATUS.getCause(), capturedErrorList.getFirst().getCause());
        Assertions.assertEquals("La notifica è in stato DELIVERED, gli stati validi sono [EFFECTIVE_DATE, RETURNED_TO_SENDER, VIEWED]", capturedErrorList.getFirst().getDescription());
    }

    @Test
    void handleCheckNotificationStatusKo_MULTI() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("ATTEMPT_0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt(), new NotificationRecipientInt()))
                .build();

        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);

        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.REFUSED);
        when(timelineService.getTimelineAndStatusHistory(any(),anyInt(),any())).thenReturn(notificationHistoryResponse);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.INVALID_NOTIFICATION_STATUS.getCause(), capturedErrorList.getFirst().getCause());
        Assertions.assertEquals("La notifica è in stato REFUSED, gli stati validi sono [DELIVERING, DELIVERED, EFFECTIVE_DATE, VIEWED, RETURNED_TO_SENDER, UNREACHABLE]", capturedErrorList.getFirst().getDescription());
    }

    @Test
    void handleNotificationTimelineKo_Invalid_Attempt() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("ATTEMPT_1");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .build();

        Set<TimelineElementInternal> timeline = new HashSet<>();
        TimelineElementInternal timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE);
        timelineElement.setElementId("PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
        timelineElement.setElementId("SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.EFFECTIVE_DATE);

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(),anyInt(),any())).thenReturn(notificationHistoryResponse);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.INVALID_ATTEMPT_ID.getCause(), capturedErrorList.getFirst().getCause());
    }

    @Test
    void handleNotificationTimelineKo_No_SEND_ANALOG_FEEDBACK() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("ATTEMPT_0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .build();

        Set<TimelineElementInternal> timeline = new HashSet<>();
        TimelineElementInternal timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE);
        timelineElement.setElementId("PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.REFINEMENT);
        timelineElement.setElementId("REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);


        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.EFFECTIVE_DATE);

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(),anyInt(),any())).thenReturn(notificationHistoryResponse);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT.getCause(), capturedErrorList.getFirst().getCause());
    }

    @Test
    void handleNotificationTimelineKo_INVALID_TIMELINE_ELEMENT_VIEWED() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("ATTEMPT_0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .build();

        Set<TimelineElementInternal> timeline = new HashSet<>();
        TimelineElementInternal timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE);
        timelineElement.setElementId("PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
        timelineElement.setElementId("SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        ScheduleRefinementDetailsInt scheduleRefinementDetails = new ScheduleRefinementDetailsInt();
        scheduleRefinementDetails.setSchedulingDate(Instant.now());
        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SCHEDULE_REFINEMENT);
        timelineElement.setElementId("SCHEDULE_REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timelineElement.setDetails(scheduleRefinementDetails);
        timeline.add(timelineElement);

        NotificationViewedCreationRequestDetailsInt detailViewed = new NotificationViewedCreationRequestDetailsInt();
        detailViewed.setEventTimestamp(Instant.now().plusSeconds(60));
        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST);
        timelineElement.setElementId("NOTIFICATION_VIEWED_CREATION_REQUEST.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timelineElement.setDetails(detailViewed);
        timeline.add(timelineElement);


        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.VIEWED);

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(),anyInt(),any())).thenReturn(notificationHistoryResponse);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT.getCause(), capturedErrorList.getFirst().getCause());
        Assertions.assertEquals("Refinement in progress", capturedErrorList.getFirst().getDescription());
    }

    @Test
    void handleNotificationTimelineKo_INVALID_TIMELINE_ELEMENT_Evento_Finale_Assente() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("ATTEMPT_0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .build();

        Set<TimelineElementInternal> timeline = new HashSet<>();
        TimelineElementInternal timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE);
        timelineElement.setElementId("PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
        timelineElement.setElementId("SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.EFFECTIVE_DATE);

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(),anyInt(),any())).thenReturn(notificationHistoryResponse);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT.getCause(), capturedErrorList.getFirst().getCause());
        Assertions.assertEquals("REFINEMENT or ANALOG_WORKFLOW_RECIPIENT_DECEASED missing", capturedErrorList.getFirst().getDescription());
    }

    @Test
    void handleNotificationExpectedFinalStatusCode_INVALID_EXPECTED_STATUS_CODE() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("ATTEMPT_0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        detail.setReworkExpectedFinalStatus("KO");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .build();

        Set<TimelineElementInternal> timeline = new HashSet<>();
        TimelineElementInternal timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE);
        timelineElement.setElementId("PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
        timelineElement.setElementId("SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.REFINEMENT);
        timelineElement.setElementId("REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE);
        timelineElement.setElementId("PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
        timelineElement.setElementId("SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.REFINEMENT);
        timelineElement.setElementId("REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1");
        timeline.add(timelineElement);


        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.EFFECTIVE_DATE);

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(),anyInt(),any())).thenReturn(notificationHistoryResponse);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.INVALID_EXPECTED_STATUS_CODE.getCause(), capturedErrorList.getFirst().getCause());
        Assertions.assertEquals("Lo stato finale atteso KO non è coerente con l'attempt ATTEMPT_0", capturedErrorList.getFirst().getDescription());
    }

    @Test
    void handleNotificationAttachments_INVALID_ATTACHMENT() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        detail.setReworkExpectedFinalStatus("OK");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        NotificationDocumentInt doc = NotificationDocumentInt.builder()
                .ref(NotificationDocumentInt.Ref.builder().key("key").build())
                .build();

        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .documents(List.of(doc))
                .build();

        Set<TimelineElementInternal> timeline = new HashSet<>();
        TimelineElementInternal timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE);
        timelineElement.setElementId("PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
        timelineElement.setElementId("SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.REFINEMENT);
        timelineElement.setElementId("REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);


        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.EFFECTIVE_DATE);

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);
        when(pnDeliveryPushWorkflowConfigs.getReworkTTLAddressRange()).thenReturn(10);
        when(pnDeliveryPushWorkflowConfigs.getNotificationReworkDocumentExpiringRange()).thenReturn(30);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(),anyInt(),any())).thenReturn(notificationHistoryResponse);

        FileDownloadResponse fileResponse = new FileDownloadResponse();
        fileResponse.setRetentionUntil(OffsetDateTime.now().plusDays(1));
        fileResponse.setKey("key");

        when(safeStorageService.getFile(any(),any(),any())).thenReturn(Mono.just(fileResponse));

        CheckAddressResponse response = new CheckAddressResponse();
        response.setFound(true);
        response.setEndValidity(Instant.now().plus(20, java.time.temporal.ChronoUnit.DAYS));
        when(checkAddressApi.checkAddress(anyString())).thenReturn(response);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.INVALID_ATTACHMENT.getCause(), capturedErrorList.getFirst().getCause());
        Assertions.assertTrue(capturedErrorList.getFirst().getDescription().contains("l'allegato key scadenza:"));
    }

    @Test
    void handleNotificationAttachments_EXPIRED_ATTACHMENT() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        detail.setReworkExpectedFinalStatus("OK");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        NotificationDocumentInt doc = NotificationDocumentInt.builder()
                .ref(NotificationDocumentInt.Ref.builder().key("key").build())
                .build();

        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .documents(List.of(doc))
                .build();

        Set<TimelineElementInternal> timeline = new HashSet<>();
        TimelineElementInternal timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE);
        timelineElement.setElementId("PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
        timelineElement.setElementId("SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.REFINEMENT);
        timelineElement.setElementId("REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);


        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.EFFECTIVE_DATE);

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);
        when(pnDeliveryPushWorkflowConfigs.getReworkTTLAddressRange()).thenReturn(10);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(),anyInt(),any())).thenReturn(notificationHistoryResponse);

        FileDownloadResponse fileResponse = new FileDownloadResponse();
        fileResponse.setRetentionUntil(OffsetDateTime.now().plusDays(1));
        fileResponse.setKey("key");

        when(safeStorageService.getFile(any(),any(),any())).thenReturn(Mono.error(new PnHttpResponseException("Document not found", HttpStatus.GONE.value())));

        CheckAddressResponse response = new CheckAddressResponse();
        response.setFound(true);
        response.setEndValidity(Instant.now().plus(20, java.time.temporal.ChronoUnit.DAYS));
        when(checkAddressApi.checkAddress(anyString())).thenReturn(response);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.EXPIRED_ATTACHMENT.getCause(), capturedErrorList.getFirst().getCause());
        Assertions.assertEquals("l'allegato non è più disponibile.", capturedErrorList.getFirst().getDescription());
    }

    @Test
    void handleNotificationAddress_EXPIRED_ANALOG_ADDRESS() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        NotificationDocumentInt doc = NotificationDocumentInt.builder()
                .ref(NotificationDocumentInt.Ref.builder().key("key").build())
                .build();

        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .documents(List.of(doc))
                .build();

        Set<TimelineElementInternal> timeline = new HashSet<>();
        TimelineElementInternal timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE);
        timelineElement.setElementId("PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
        timelineElement.setElementId("SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.REFINEMENT);
        timelineElement.setElementId("REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);


        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.EFFECTIVE_DATE);

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);
        when(pnDeliveryPushWorkflowConfigs.getReworkTTLAddressRange()).thenReturn(10);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(),anyInt(),any())).thenReturn(notificationHistoryResponse);

        FileDownloadResponse fileResponse = new FileDownloadResponse();
        fileResponse.setRetentionUntil(OffsetDateTime.now().plusDays(120));

        when(safeStorageService.getFile(any(),any(),any())).thenReturn(Mono.just(fileResponse));

        CheckAddressResponse response = new CheckAddressResponse();
        response.setFound(false);
        response.setEndValidity(Instant.now().plus(20, java.time.temporal.ChronoUnit.DAYS));
        when(checkAddressApi.checkAddress(anyString())).thenReturn(response);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.EXPIRED_ANALOG_ADDRESS.getCause(), capturedErrorList.getFirst().getCause());
        Assertions.assertEquals("Indirizzo non trovato", capturedErrorList.getFirst().getDescription());

    }

    @Test
    void handleNotificationAddress_INVALID_ANALOG_ADDRESS() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        NotificationDocumentInt doc = NotificationDocumentInt.builder()
                .ref(NotificationDocumentInt.Ref.builder().key("key").build())
                .build();

        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .documents(List.of(doc))
                .build();

        Set<TimelineElementInternal> timeline = new HashSet<>();
        TimelineElementInternal timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE);
        timelineElement.setElementId("PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
        timelineElement.setElementId("SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.REFINEMENT);
        timelineElement.setElementId("REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);


        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.EFFECTIVE_DATE);

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);
        when(pnDeliveryPushWorkflowConfigs.getReworkTTLAddressRange()).thenReturn(10);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(),anyInt(),any())).thenReturn(notificationHistoryResponse);

        FileDownloadResponse fileResponse = new FileDownloadResponse();
        fileResponse.setRetentionUntil(OffsetDateTime.now().plusDays(120));

        when(safeStorageService.getFile(any(),any(),any())).thenReturn(Mono.just(fileResponse));

        CheckAddressResponse response = new CheckAddressResponse();
        response.setFound(true);
        response.setEndValidity(Instant.now().plus(5, java.time.temporal.ChronoUnit.DAYS));
        when(checkAddressApi.checkAddress(anyString())).thenReturn(response);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.INVALID_ANALOG_ADDRESS.getCause(), capturedErrorList.getFirst().getCause());
        Assertions.assertEquals("L'indirizzo trovato ma scade nel " + DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(response.getEndValidity()), capturedErrorList.getFirst().getDescription());

    }
}