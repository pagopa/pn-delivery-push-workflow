package it.pagopa.pn.deliverypushworkflow.action.rework;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkValidationDetails;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkError;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkErrorCause;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.ReworkRequestTypeEnum;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.*;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.actionmanager.api.ActionApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.actionmanager.model.NewAction;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.model.CheckAddressResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationStatus;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationStatusHistoryInvalidatedElement;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.SendAnalogFeedbackDetails;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.paperchannel.PaperChannelAddressClient;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventAction;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventPool;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.SafeStorageService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import it.pagopa.pn.deliverypushworkflow.dto.ext.externalchannel.ResponseStatusInt;

import static org.mockito.Mockito.*;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class ReworkValidationHandlerTest {

    @Mock
    private PaperChannelAddressClient paperChannelAddressClient;
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
    @Mock
    private AttachmentUtils attachmentUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();


    private ReworkValidationHandler notificationReworkHandler;

    @BeforeEach
    void setup() {
        notificationReworkHandler = new ReworkValidationHandler(paperChannelAddressClient, actionManagerApi, notificationService, timelineService, timelineUtils, reworkRequestEventPool, pnDeliveryPushWorkflowConfigs, safeStorageService, objectMapper, attachmentUtils);
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
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(),anyInt(),any())).thenReturn(notificationHistoryResponse);

        FileDownloadResponse fileResponse = new FileDownloadResponse();
        fileResponse.setRetentionUntil(OffsetDateTime.now().plusDays(120));

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
    void handleNotificationTimelineKo_INVALID_ATTEMPT_1_VIEWED() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("ATTEMPT_0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        detail.setRequestType(ReworkRequestTypeEnum.REWORK);
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
        timelineElement.setElementId("PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
        timelineElement.setElementId("SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1");
        timeline.add(timelineElement);

        ScheduleRefinementDetailsInt scheduleRefinementDetails = new ScheduleRefinementDetailsInt();
        scheduleRefinementDetails.setSchedulingDate(Instant.now());
        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SCHEDULE_REFINEMENT);
        timelineElement.setElementId("SCHEDULE_REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1");
        timelineElement.setDetails(scheduleRefinementDetails);
        timeline.add(timelineElement);

        NotificationViewedCreationRequestDetailsInt detailViewed = new NotificationViewedCreationRequestDetailsInt();
        detailViewed.setEventTimestamp(Instant.now().plusSeconds(60));
        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST);
        timelineElement.setElementId("NOTIFICATION_VIEWED_CREATION_REQUEST.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0");
        timelineElement.setDetails(detailViewed);
        timeline.add(timelineElement);


        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.VIEWED);

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(timelineUtils.checkIsNotificationViewed(any(), any())).thenReturn(true);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(),anyInt(),any())).thenReturn(notificationHistoryResponse);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.INVALID_NOTIFICATION_STATUS.getCause(), capturedErrorList.getFirst().getCause());
        Assertions.assertEquals("Invalid status VIEWED if ATTEMPT_1 exists", capturedErrorList.getFirst().getDescription());
    }

    @Test
    void handleNotificationTimelineKO_INVALID_ATTEMPT_1_REQ_ATTEMPT_1_VIEWED() {
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
        timelineElement.setElementId("PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
        timelineElement.setElementId("SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1");
        timeline.add(timelineElement);

        ScheduleRefinementDetailsInt scheduleRefinementDetails = new ScheduleRefinementDetailsInt();
        scheduleRefinementDetails.setSchedulingDate(Instant.now());
        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SCHEDULE_REFINEMENT);
        timelineElement.setElementId("SCHEDULE_REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1");
        timelineElement.setDetails(scheduleRefinementDetails);
        timeline.add(timelineElement);

        NotificationViewedCreationRequestDetailsInt detailViewed = new NotificationViewedCreationRequestDetailsInt();
        detailViewed.setEventTimestamp(Instant.now().plusSeconds(60));
        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST);
        timelineElement.setElementId("NOTIFICATION_VIEWED_CREATION_REQUEST.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0");
        timelineElement.setDetails(detailViewed);
        timeline.add(timelineElement);


        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.VIEWED);

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(timelineUtils.checkIsNotificationViewed(any(), any())).thenReturn(true);
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
        when(timelineUtils.checkIsNotificationViewed(any(), any())).thenReturn(true);
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
        timelineElement.setCategory(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE);
        timelineElement.setElementId("SEND_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1");
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
        Assertions.assertEquals("Non è possibile correggere l'ATTEMPT_0 di una notifica con un KO se l'ATTEMPT_1 è già presente", capturedErrorList.getFirst().getDescription());
    }

    @Test
    void handleNotificationExpectedFinalStatusCode_containsInvalidatedAttempt1() {
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
        timelineElement.setCategory(TimelineElementCategoryInt.NOTIFICATION_TIMELINE_REWORKED);
        timelineElement.setElementId("NOTIFICATION_TIMELINE_REWORKED.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        NotificationStatusHistoryInvalidatedElement element = new NotificationStatusHistoryInvalidatedElement();
        element.setRelatedTimelineElementIds(List.of("SEND_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1"));
        timelineElement.setDetails(NotificationTimelineReworkedDetailsInt.builder()
                .invalidatedTimelineAndStatusHistory(List.of(element)).build());
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
        Assertions.assertEquals("Non è possibile correggere l'ATTEMPT_0 di una notifica con un KO se l'ATTEMPT_1 è già presente", capturedErrorList.getFirst().getDescription());
    }

    @Test
    void handleNotificationAttachments_INVALID_ATTACHMENT() {
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
        response.setEndValidity(Instant.now().plus(20, java.time.temporal.ChronoUnit.DAYS));
        when(paperChannelAddressClient.checkAddress(anyString())).thenReturn(Mono.just(response));

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
        detail.setReworkAttempt("ATTEMPT_0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        detail.setReworkExpectedFinalStatus("KO");
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

        when(safeStorageService.getFile(any(),any(),any())).thenReturn(Mono.error(new WebClientResponseException(HttpStatus.GONE.value(), "Document not found", null, null, null)));

        CheckAddressResponse response = new CheckAddressResponse();
        response.setEndValidity(Instant.now().plus(20, java.time.temporal.ChronoUnit.DAYS));
        when(paperChannelAddressClient.checkAddress(anyString())).thenReturn(Mono.just(response));

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
        detail.setReworkAttempt("ATTEMPT_0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        detail.setReworkExpectedFinalStatus("KO");
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
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(),anyInt(),any())).thenReturn(notificationHistoryResponse);

        FileDownloadResponse fileResponse = new FileDownloadResponse();
        fileResponse.setRetentionUntil(OffsetDateTime.now().plusDays(120));

        when(safeStorageService.getFile(any(),any(),any())).thenReturn(Mono.just(fileResponse));

        CheckAddressResponse response = new CheckAddressResponse();
        response.setEndValidity(Instant.now().plus(20, java.time.temporal.ChronoUnit.DAYS));
        when(paperChannelAddressClient.checkAddress(anyString())).thenReturn(Mono.error(new PnHttpResponseException("empty", 404)));

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
        detail.setReworkAttempt("ATTEMPT_0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        detail.setReworkExpectedFinalStatus("KO");
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
        response.setEndValidity(Instant.now().plus(5, java.time.temporal.ChronoUnit.DAYS));
        when(paperChannelAddressClient.checkAddress(anyString())).thenReturn(Mono.just(response));

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.INVALID_ANALOG_ADDRESS.getCause(), capturedErrorList.getFirst().getCause());
        Assertions.assertEquals("L'indirizzo trovato ma scade nel " + DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(response.getEndValidity()), capturedErrorList.getFirst().getDescription());

    }

    @Test
    void handleNotificationAddress_InvalidAnalogAddress() {
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
        timelineElement.setCategory(TimelineElementCategoryInt.NOTIFICATION_TIMELINE_REWORKED);
        timelineElement.setElementId("NOTIFICATION_TIMELINE_REWORKED.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        NotificationStatusHistoryInvalidatedElement element = new NotificationStatusHistoryInvalidatedElement();
        element.setRelatedTimelineElementIds(List.of("SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1"));
        timelineElement.setDetails(NotificationTimelineReworkedDetailsInt.builder()
                .invalidatedTimelineAndStatusHistory(List.of(element)).build());
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
        response.setEndValidity(Instant.now().plus(5, java.time.temporal.ChronoUnit.DAYS));
        when(paperChannelAddressClient.checkAddress(anyString())).thenReturn(Mono.just(response));

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.INVALID_ANALOG_ADDRESS.getCause(), capturedErrorList.getFirst().getCause());
        Assertions.assertEquals("L'indirizzo trovato ma scade nel " + DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(response.getEndValidity()), capturedErrorList.getFirst().getDescription());

    }

    @Test
    void handleNotificationExpectedFinalStatusCode_RESTART_skipCheck() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("ATTEMPT_0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        detail.setReworkExpectedFinalStatus("KO");
        detail.setRequestType(ReworkRequestTypeEnum.RESTART);

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

        // Se il check non fosse bypassato, questa presenza farebbe fallire il KO su ATTEMPT_0.
        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE);
        timelineElement.setElementId("SEND_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1");
        timeline.add(timelineElement);

        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.EFFECTIVE_DATE);

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);
        when(pnDeliveryPushWorkflowConfigs.getReworkTTLAddressRange()).thenReturn(10);
        when(pnDeliveryPushWorkflowConfigs.getNotificationReworkDocumentExpiringRange()).thenReturn(30);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(), anyInt(), any())).thenReturn(notificationHistoryResponse);

        FileDownloadResponse fileResponse = new FileDownloadResponse();
        fileResponse.setRetentionUntil(OffsetDateTime.now().plusDays(120));
        when(safeStorageService.getFile(any(), any(), any())).thenReturn(Mono.just(fileResponse));

        CheckAddressResponse response = new CheckAddressResponse();
        response.setEndValidity(Instant.now().plus(20, java.time.temporal.ChronoUnit.DAYS));
        when(paperChannelAddressClient.checkAddress(anyString())).thenReturn(Mono.just(response));

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, times(1)).insertAction(any());
        verify(reworkRequestEventPool, never()).scheduleFutureAction(any(), any());
    }

    @Test
    void handleNotificationTimelineKo_RESTART_PAYMENT_CategoryFound() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("ATTEMPT_0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        detail.setRequestType(ReworkRequestTypeEnum.RESTART);

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
        timelineElement.setCategory(TimelineElementCategoryInt.PAYMENT);
        timelineElement.setElementId("NOTIFICATION_PAID.IUN_AJDN-ZDVK-UGMU-202605-E-1.CODE_PPA30201140004608200077777777777");
        timelineElement.setDetails(NotificationPaidDetailsInt.builder().recIndex(0).build());
        timeline.add(timelineElement);

        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.EFFECTIVE_DATE);

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(), anyInt(), any())).thenReturn(notificationHistoryResponse);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT.getCause(), capturedErrorList.getFirst().getCause());
        Assertions.assertEquals("PAYMENT category found in timeline", capturedErrorList.getFirst().getDescription());
    }

    @Test
    void handleNotificationReworkWithViewed_attachmentsExistOnViewed() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("ATTEMPT_0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        detail.setRequestType(ReworkRequestTypeEnum.RESTART);

        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .documents(List.of(NotificationDocumentInt.builder()
                        .ref(NotificationDocumentInt.Ref.builder().key("key").build())
                        .build()))
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
        timelineElement.setCategory(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST);
        timelineElement.setElementId("NOTIFICATION_VIEWED_CREATION_REQUEST.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.NOTIFICATION_VIEWED);
        timelineElement.setElementId("NOTIFICATION_VIEWED.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.REFINEMENT);
        timelineElement.setElementId("REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.EFFECTIVE_DATE);

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(timelineUtils.checkIsNotificationViewed(any(), any())).thenReturn(true);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(), anyInt(), any())).thenReturn(notificationHistoryResponse);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.INVALID_VIEWED_ELEMENTS.getCause(), capturedErrorList.getFirst().getCause());
        Assertions.assertEquals("Non è possibile procedere alla richiesta di correzione, la visualizzazione non è invalidabile", capturedErrorList.getFirst().getDescription());
    }

    @Test
    void handleNotificationReworkWithViewed_insertActionContainsRequestType() throws Exception {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkId("RWK-123");
        detail.setReworkAttempt("ATTEMPT_0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        detail.setReworkExpectedFinalStatus("OK");
        detail.setRequestType(ReworkRequestTypeEnum.RESTART);

        Action action = Action.builder()
                .actionId("ACTION-123")
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
        timelineElement.setCategory(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST);
        timelineElement.setElementId("NOTIFICATION_VIEWED_CREATION_REQUEST.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.NOTIFICATION_VIEWED);
        timelineElement.setElementId("NOTIFICATION_VIEWED.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.REFINEMENT);
        timelineElement.setElementId("REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.EFFECTIVE_DATE);

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);
        when(pnDeliveryPushWorkflowConfigs.getNotificationReworkDocumentExpiringRange()).thenReturn(30);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(), anyInt(), any())).thenReturn(notificationHistoryResponse);

        FileDownloadResponse fileResponse = new FileDownloadResponse();
        fileResponse.setRetentionUntil(OffsetDateTime.now().plusDays(120));
        fileResponse.setKey("key");
        when(safeStorageService.getFile(any(), any(), any())).thenReturn(Mono.just(fileResponse));

        ArgumentCaptor<NewAction> captor = ArgumentCaptor.forClass(NewAction.class);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, times(1)).insertAction(captor.capture());
        verify(reworkRequestEventPool, never()).scheduleFutureAction(any(), any());

        JsonNode insertedDetails = objectMapper.readTree(captor.getValue().getDetails());

        Assertions.assertEquals(detail.getRequestType().name(), insertedDetails.path("requestType").asText());
    }

    @Test
    void handleNotificationRework_insertActionContainsRequestType() throws Exception {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkId("RWK-123");
        detail.setReworkAttempt("ATTEMPT_0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        detail.setReworkExpectedFinalStatus("OK");
        detail.setRequestType(ReworkRequestTypeEnum.RESTART);

        Action action = Action.builder()
                .actionId("ACTION-123")
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
        when(pnDeliveryPushWorkflowConfigs.getNotificationReworkDocumentExpiringRange()).thenReturn(30);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimelineAndStatusHistory(any(), anyInt(), any())).thenReturn(notificationHistoryResponse);

        FileDownloadResponse fileResponse = new FileDownloadResponse();
        fileResponse.setRetentionUntil(OffsetDateTime.now().plusDays(120));
        fileResponse.setKey("key");
        when(safeStorageService.getFile(any(), any(), any())).thenReturn(Mono.just(fileResponse));

        ArgumentCaptor<NewAction> captor = ArgumentCaptor.forClass(NewAction.class);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, times(1)).insertAction(captor.capture());
        verify(reworkRequestEventPool, never()).scheduleFutureAction(any(), any());

        JsonNode insertedDetails = objectMapper.readTree(captor.getValue().getDetails());

        Assertions.assertEquals(detail.getRequestType().name(), insertedDetails.path("requestType").asText());
    }

    @Test
    void handleNotificationInvalidateElements_INVALID_CATEGORY_TO_INVALIDATE() {
        NotificationReworkValidationDetails detail = baseInvalidateElementsDetail();
        detail.setElementsToInvalidate(List.of("REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0"));

        Action action = baseAction(detail);
        NotificationInt notification = baseNotification();

        Set<TimelineElementInternal> timeline = validInvalidateTimeline();

        mockBaseValidFlow(notification, timeline);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool).scheduleFutureAction(captor.capture(), any());

        Assertions.assertTrue(captor.getValue().getError().stream()
                .anyMatch(e -> NotificationReworkErrorCause.INVALID_CATEGORY_TO_INVALIDATE.getCause().equals(e.getCause())));
    }


    @Test
    void handleNotificationInvalidateElements_INVALID_CATEGORY_TO_INVALIDATE2() {
        NotificationReworkValidationDetails detail = baseInvalidateElementsDetail();
        detail.setElementsToInvalidate(List.of("ANALOG_SUCCESS_WORKFLOW.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0"));

        Action action = baseAction(detail);
        NotificationInt notification = baseNotification();

        Set<TimelineElementInternal> timeline = validInvalidateTimeline();

        mockBaseValidFlow(notification, timeline);
        timeline.add(timelineElement(
                TimelineElementCategoryInt.ANALOG_SUCCESS_WORKFLOW,
                "ANALOG_SUCCESS_WORKFLOW.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                AnalogSuccessWorkflowDetailsInt.builder().recIndex(0).build()
        ));

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool).scheduleFutureAction(captor.capture(), any());

        Assertions.assertTrue(captor.getValue().getError().stream()
                .anyMatch(e -> NotificationReworkErrorCause.INVALID_CATEGORY_TO_INVALIDATE.getCause().equals(e.getCause())));
    }

    @Test
    void handleNotificationInvalidateElements_INVALID_ATTEMPT0_ELEMENT() {
        NotificationReworkValidationDetails detail = baseInvalidateElementsDetail();
        detail.setElementsToInvalidate(List.of(
                "SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0"
        ));

        Action action = baseAction(detail);
        NotificationInt notification = baseNotification();

        Set<TimelineElementInternal> timeline = validInvalidateTimeline();

        mockBaseValidFlow(notification, timeline);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool).scheduleFutureAction(captor.capture(), any());

        Assertions.assertTrue(captor.getValue().getError().stream()
                .anyMatch(e -> NotificationReworkErrorCause.INVALID_CATEGORY_TO_INVALIDATE.getCause().equals(e.getCause())));
    }

    @Test
    void handleNotificationInvalidateElements_INVALID_ATTEMPT1_ELEMENT_withoutAttempt0Ok() {
        NotificationReworkValidationDetails detail = baseInvalidateElementsDetail();
        detail.setElementsToInvalidate(List.of(
                "SEND_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1"
        ));

        Action action = baseAction(detail);
        NotificationInt notification = baseNotification();

        Set<TimelineElementInternal> timeline = validInvalidateTimelineWithoutOkAttempt0();
        timeline.add(timelineElement(
                TimelineElementCategoryInt.SEND_ANALOG_DOMICILE,
                "SEND_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1",
                SendAnalogDetailsInt.builder().recIndex(0).sentAttemptMade(1).build()
        ));
        timeline.add(timelineElement(
                TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK,
                "SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0",
                SendAnalogFeedbackDetailsInt.builder().recIndex(0).sentAttemptMade(0).responseStatus(ResponseStatusInt.KO).build()
        ));

        mockBaseValidFlow(notification, timeline);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool).scheduleFutureAction(captor.capture(), any());

        Assertions.assertTrue(captor.getValue().getError().stream()
                .anyMatch(e -> NotificationReworkErrorCause.INVALID_CATEGORY_TO_INVALIDATE.getCause().equals(e.getCause())));
    }

    @Test
    void handleNotificationInvalidateElements_INVALID_ATTEMPT1_ELEMENTS_whenOtherAttempt1Remains() {
        NotificationReworkValidationDetails detail = baseInvalidateElementsDetail();
        detail.setElementsToInvalidate(List.of(
                "SEND_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1"
        ));

        Action action = baseAction(detail);
        NotificationInt notification = baseNotification();

        Set<TimelineElementInternal> timeline = validInvalidateTimeline();
        timeline.add(timelineElement(
                TimelineElementCategoryInt.SEND_ANALOG_DOMICILE,
                "SEND_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1",
                SendAnalogDetailsInt.builder().recIndex(0).sentAttemptMade(1).build()

        ));
        timeline.add(timelineElement(
                TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE,
                "PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1",
                BaseAnalogDetailsInt.builder().recIndex(0).sentAttemptMade(1).build()

        ));
        timeline.add(timelineElement(
                TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK,
                "SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0",
                SendAnalogFeedbackDetailsInt.builder().recIndex(0).sentAttemptMade(0).responseStatus(ResponseStatusInt.OK).build()

        ));

        mockBaseValidFlow(notification, timeline);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool).scheduleFutureAction(captor.capture(), any());

        Assertions.assertTrue(captor.getValue().getError().stream()
                .anyMatch(e -> NotificationReworkErrorCause.INVALID_CATEGORY_TO_INVALIDATE.getCause().equals(e.getCause())));
    }

    @Test
    void handleNotificationInvalidateElements_INVALID_CATEGORY_TO_INVALIDATE_SEND_ANALOG() {
        NotificationReworkValidationDetails detail = baseInvalidateElementsDetail();
        detail.setElementsToInvalidate(List.of(
                "SEND_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1"
        ));

        Action action = baseAction(detail);
        NotificationInt notification = baseNotification();

        Set<TimelineElementInternal> timeline = validInvalidateTimeline();
        timeline.add(timelineElement(
                TimelineElementCategoryInt.SEND_ANALOG_DOMICILE,
                "SEND_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1",
                SendAnalogDetailsInt.builder().recIndex(0).sentAttemptMade(1).build()

        ));
        timeline.add(timelineElement(
                TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK,
                "SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0",
                SendAnalogFeedbackDetailsInt.builder().recIndex(0).sentAttemptMade(0).responseStatus(ResponseStatusInt.OK).build()

        ));

        mockBaseValidFlow(notification, timeline);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool).scheduleFutureAction(captor.capture(), any());

        Assertions.assertTrue(captor.getValue().getError().stream()
                .anyMatch(e -> NotificationReworkErrorCause.INVALID_CATEGORY_TO_INVALIDATE.getCause().equals(e.getCause())));
    }

    @Test
    void handleNotificationInvalidateElements_VIEWED_invalidWhenViewedElementRemains() {
        NotificationReworkValidationDetails detail = baseInvalidateElementsDetail();
        detail.setElementsToInvalidate(List.of(
                "NOTIFICATION_VIEWED.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0"
        ));

        Action action = baseAction(detail);
        NotificationInt notification = baseNotification();

        Set<TimelineElementInternal> timeline = validInvalidateTimeline();
        timeline.add(timelineElement(
                TimelineElementCategoryInt.NOTIFICATION_VIEWED,
                "NOTIFICATION_VIEWED.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                NotificationViewedDetailsInt.builder().recIndex(0).build()
        ));
        timeline.add(timelineElement(
                TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST,
                "NOTIFICATION_VIEWED_CREATION_REQUEST.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                NotificationViewedCreationRequestDetailsInt.builder().recIndex(0).eventTimestamp(Instant.now()).build()
        ));

        mockBaseValidFlow(notification, timeline);

        WebClientResponseException exception = mock(WebClientResponseException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.GONE);
        when(exception.getResponseBodyAsString()).thenReturn("[deletionTimestamp=2010-06-24T10:15:30Z]");
        when(attachmentUtils.getAllAttachmentsForSpecificRecipient(any(), anyString())).thenReturn(List.of(NotificationDocumentInt.builder()
                .ref(NotificationDocumentInt.Ref.builder().key("key").build()).build()));
        when(safeStorageService.getFile(any(), any(), any())).thenReturn(Mono.error(exception));

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool).scheduleFutureAction(captor.capture(), any());

        Assertions.assertTrue(captor.getValue().getError().stream()
                .anyMatch(e -> NotificationReworkErrorCause.INVALID_VIEWED_ELEMENT.getCause().equals(e.getCause())));
    }

    @Test
    void handleNotificationInvalidateElements_VIEWED_validWhenNoViewedElementRemains() {
        NotificationReworkValidationDetails detail = baseInvalidateElementsDetail();
        detail.setElementsToInvalidate(List.of(
                "NOTIFICATION_VIEWED.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                "NOTIFICATION_VIEWED_CREATION_REQUEST.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0"
        ));

        Action action = baseAction(detail);
        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .documents(List.of(NotificationDocumentInt.builder()
                        .ref(NotificationDocumentInt.Ref.builder().key("key").build())
                        .build()))
                .build();

        Set<TimelineElementInternal> timeline = validInvalidateTimeline();
        timeline.add(timelineElement(
                TimelineElementCategoryInt.NOTIFICATION_VIEWED,
                "NOTIFICATION_VIEWED.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                NotificationViewedDetailsInt.builder().recIndex(0).build()
        ));
        timeline.add(timelineElement(
                TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST,
                "NOTIFICATION_VIEWED_CREATION_REQUEST.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                NotificationViewedCreationRequestDetailsInt.builder().recIndex(0).eventTimestamp(Instant.now()).build()
        ));

        WebClientResponseException exception = mock(WebClientResponseException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.GONE);
        when(exception.getResponseBodyAsString()).thenReturn("[deletionTimestamp=2010-06-24T10:15:30Z]");
        when(attachmentUtils.getAllAttachmentsForSpecificRecipient(any(), anyString())).thenReturn(List.of(NotificationDocumentInt.builder()
                .ref(NotificationDocumentInt.Ref.builder().key("key").build()).build()));
        when(safeStorageService.getFile(any(), any(), any())).thenReturn(Mono.error(exception));

        mockBaseValidFlow(notification, timeline);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(safeStorageService, times(1)).getFile(any(), any(), any());
        verify(actionManagerApi).insertAction(any());
        verify(reworkRequestEventPool, never()).scheduleFutureAction(any(), any());
    }

    @Test
    void handleNotificationInvalidateElements_VIEWED_AttachmentsPresentOnViewed() {
        NotificationReworkValidationDetails detail = baseInvalidateElementsDetail();
        detail.setElementsToInvalidate(List.of(
                "NOTIFICATION_VIEWED.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                "NOTIFICATION_VIEWED_CREATION_REQUEST.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0"
        ));

        Action action = baseAction(detail);
        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .documents(List.of(NotificationDocumentInt.builder()
                        .ref(NotificationDocumentInt.Ref.builder().key("key").build())
                        .build()))
                .build();

        Set<TimelineElementInternal> timeline = validInvalidateTimeline();
        timeline.add(timelineElement(
                TimelineElementCategoryInt.NOTIFICATION_VIEWED,
                "NOTIFICATION_VIEWED.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                NotificationViewedDetailsInt.builder().recIndex(0).eventTimestamp(Instant.EPOCH).build()
        ));
        timeline.add(timelineElement(
                TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST,
                "NOTIFICATION_VIEWED_CREATION_REQUEST.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                NotificationViewedCreationRequestDetailsInt.builder().recIndex(0).eventTimestamp(Instant.EPOCH).build()
        ));

        mockBaseValidFlow(notification, timeline);
        when(attachmentUtils.getAllAttachmentsForSpecificRecipient(any(), anyString())).thenReturn(List.of(NotificationDocumentInt.builder()
                .ref(NotificationDocumentInt.Ref.builder().key("key").build()).build()));
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setRetentionUntil(OffsetDateTime.now().plusDays(120));
        when(safeStorageService.getFile(any(), any(), any())).thenReturn(Mono.just(fileDownloadResponse));

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool).scheduleFutureAction(captor.capture(), any());

        Assertions.assertTrue(captor.getValue().getError().stream()
                .anyMatch(e -> NotificationReworkErrorCause.INVALID_ELEMENT_TO_INVALIDATE_ATTACHMENTS_EXIST_ONVIEWED.getCause().equals(e.getCause())));
    }

    @Test
    void handleNotificationInvalidateElements_VIEWED_AllAttachmentsPresentOnViewed() {
        NotificationReworkValidationDetails detail = baseInvalidateElementsDetail();
        detail.setElementsToInvalidate(List.of(
                "NOTIFICATION_VIEWED.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                "NOTIFICATION_VIEWED_CREATION_REQUEST.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0"
        ));

        Action action = baseAction(detail);
        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .documents(List.of(NotificationDocumentInt.builder()
                        .ref(NotificationDocumentInt.Ref.builder().key("key").build())
                        .build(), NotificationDocumentInt.builder()
                        .ref(NotificationDocumentInt.Ref.builder().key("key2").build())
                        .build()))
                .build();

        Set<TimelineElementInternal> timeline = validInvalidateTimeline();
        timeline.add(timelineElement(
                TimelineElementCategoryInt.NOTIFICATION_VIEWED,
                "NOTIFICATION_VIEWED.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                NotificationViewedDetailsInt.builder().recIndex(0).eventTimestamp(Instant.EPOCH).build()
        ));
        timeline.add(timelineElement(
                TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST,
                "NOTIFICATION_VIEWED_CREATION_REQUEST.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                NotificationViewedCreationRequestDetailsInt.builder().recIndex(0).eventTimestamp(Instant.EPOCH).build()
        ));

        mockBaseValidFlow(notification, timeline);
        when(attachmentUtils.getAllAttachmentsForSpecificRecipient(any(), anyString())).thenReturn(List.of(NotificationDocumentInt.builder()
                .ref(NotificationDocumentInt.Ref.builder().key("key").build()).build()));
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setRetentionUntil(OffsetDateTime.now().plusDays(120));
        when(safeStorageService.getFile(any(), any(), any())).thenReturn(Mono.just(fileDownloadResponse));

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool).scheduleFutureAction(captor.capture(), any());

        Assertions.assertTrue(captor.getValue().getError().stream()
                .anyMatch(e -> NotificationReworkErrorCause.INVALID_ELEMENT_TO_INVALIDATE_ATTACHMENTS_EXIST_ONVIEWED.getCause().equals(e.getCause())));
    }

    @Test
    void handleNotificationInvalidateElements_VIEWED_OnlyOneAttachmentsPresentOnViewed() {
        NotificationReworkValidationDetails detail = baseInvalidateElementsDetail();
        detail.setElementsToInvalidate(List.of(
                "NOTIFICATION_VIEWED.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                "NOTIFICATION_VIEWED_CREATION_REQUEST.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0"
        ));

        Action action = baseAction(detail);
        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .documents(List.of(NotificationDocumentInt.builder()
                        .ref(NotificationDocumentInt.Ref.builder().key("key").build())
                        .build(), NotificationDocumentInt.builder()
                        .ref(NotificationDocumentInt.Ref.builder().key("key2").build())
                        .build()))
                .build();

        Set<TimelineElementInternal> timeline = validInvalidateTimeline();
        timeline.add(timelineElement(
                TimelineElementCategoryInt.NOTIFICATION_VIEWED,
                "NOTIFICATION_VIEWED.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                NotificationViewedDetailsInt.builder().recIndex(0).build()
        ));
        timeline.add(timelineElement(
                TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST,
                "NOTIFICATION_VIEWED_CREATION_REQUEST.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                NotificationViewedCreationRequestDetailsInt.builder().recIndex(0).eventTimestamp(Instant.now()).build()
        ));

        mockBaseValidFlow(notification, timeline);
        when(attachmentUtils.getAllAttachmentsForSpecificRecipient(any(), anyString())).thenReturn(List.of(NotificationDocumentInt.builder()
                .ref(NotificationDocumentInt.Ref.builder().key("key").build()).build(), NotificationDocumentInt.builder()
                .ref(NotificationDocumentInt.Ref.builder().key("key2").build()).build()));
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setRetentionUntil(OffsetDateTime.now().plusDays(120));
        WebClientResponseException exception = mock(WebClientResponseException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.GONE);
        when(exception.getResponseBodyAsString()).thenReturn("[deletionTimestamp=2010-06-24T10:15:30Z]");
        when(safeStorageService.getFile(any(), any(), any())).thenReturn(Mono.just(fileDownloadResponse)).thenReturn(Mono.error(exception));

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(safeStorageService, times(2)).getFile(any(), any(), any());
        verify(actionManagerApi).insertAction(any());
        verify(reworkRequestEventPool, never()).scheduleFutureAction(any(), any());
    }

    @Test
    void handleNotificationInvalidateElements_VIEWED_AttachmentsRemovedAfterViewed() {
        NotificationReworkValidationDetails detail = baseInvalidateElementsDetail();
        detail.setElementsToInvalidate(List.of(
                "NOTIFICATION_VIEWED.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                "NOTIFICATION_VIEWED_CREATION_REQUEST.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0"
        ));

        Action action = baseAction(detail);
        NotificationInt notification = NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .documents(List.of(NotificationDocumentInt.builder()
                        .ref(NotificationDocumentInt.Ref.builder().key("key").build())
                        .build(), NotificationDocumentInt.builder()
                        .ref(NotificationDocumentInt.Ref.builder().key("key2").build())
                        .build()))
                .build();

        Set<TimelineElementInternal> timeline = validInvalidateTimeline();
        timeline.add(timelineElement(
                TimelineElementCategoryInt.NOTIFICATION_VIEWED,
                "NOTIFICATION_VIEWED.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                NotificationViewedDetailsInt.builder().recIndex(0).build()
        ));
        timeline.add(timelineElement(
                TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST,
                "NOTIFICATION_VIEWED_CREATION_REQUEST.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                NotificationViewedCreationRequestDetailsInt.builder().recIndex(0).eventTimestamp(Instant.now()).build()
        ));

        mockBaseValidFlow(notification, timeline);
        when(attachmentUtils.getAllAttachmentsForSpecificRecipient(any(), anyString())).thenReturn(List.of(NotificationDocumentInt.builder()
                .ref(NotificationDocumentInt.Ref.builder().key("key").build()).build(), NotificationDocumentInt.builder()
                .ref(NotificationDocumentInt.Ref.builder().key("key2").build()).build()));
        WebClientResponseException exception = mock(WebClientResponseException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.GONE);
        when(exception.getResponseBodyAsString()).thenReturn("[deletionTimestamp=2099-06-24T10:15:30Z]");
        when(safeStorageService.getFile(any(), any(), any())).thenReturn(Mono.error(exception));

        notificationReworkHandler.handleNotificationRework(action).block();
        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool).scheduleFutureAction(captor.capture(), any());

        Assertions.assertTrue(captor.getValue().getError().stream()
                .anyMatch(e -> NotificationReworkErrorCause.INVALID_ELEMENT_TO_INVALIDATE_ATTACHMENTS_EXIST_ONVIEWED.getCause().equals(e.getCause())));
    }

    @Test
    void handleNotificationInvalidateElements_INVALID_REC_INDEX() {
        NotificationReworkValidationDetails detail = baseInvalidateElementsDetail();
        detail.setElementsToInvalidate(List.of(
                "SEND_ANALOG_PROGRESS.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_1.ATTEMPT_0"
        ));

        Action action = baseAction(detail);
        NotificationInt notification = baseNotification();

        Set<TimelineElementInternal> timeline = validInvalidateTimeline();

        mockBaseValidFlow(notification, timeline);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool).scheduleFutureAction(captor.capture(), any());

        Assertions.assertTrue(captor.getValue().getError().stream()
                .anyMatch(e -> NotificationReworkErrorCause.INVALID_REC_INDEX.getCause().equals(e.getCause())));
    }


    @Test
    void handleNotificationInvalidateElements_INVALID_PROGRESS_ELEMENT() {
        NotificationReworkValidationDetails detail = baseInvalidateElementsDetail();
        detail.setElementsToInvalidate(List.of(
                "SEND_ANALOG_PROGRESS.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_1.ATTEMPT_0",
                "PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0"
        ));

        Action action = baseAction(detail);
        NotificationInt notification = baseNotification();

        Set<TimelineElementInternal> timeline = validInvalidateTimeline();

        mockBaseValidFlow(notification, timeline);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi, never()).insertAction(any());

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool).scheduleFutureAction(captor.capture(), any());

        Assertions.assertTrue(captor.getValue().getError().stream()
                .anyMatch(e -> NotificationReworkErrorCause.INVALID_PROGRESS_ELEMENT.getCause().equals(e.getCause())));
    }


    @Test
    void handleNotificationInvalidateElements_SEND_ANALOG_PROGRESS_valid() {
        NotificationReworkValidationDetails detail = baseInvalidateElementsDetail();
        detail.setElementsToInvalidate(List.of(
                "SEND_ANALOG_PROGRESS.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0"
        ));

        Action action = baseAction(detail);
        NotificationInt notification = baseNotification();

        Set<TimelineElementInternal> timeline = validInvalidateTimeline();

        mockBaseValidFlow(notification, timeline);

        notificationReworkHandler.handleNotificationRework(action).block();

        verify(actionManagerApi).insertAction(any());
        verify(reworkRequestEventPool, never()).scheduleFutureAction(any(), any());
    }

    private NotificationReworkValidationDetails baseInvalidateElementsDetail() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkId("RWK-INVALIDATE");
        detail.setReworkAttempt("ATTEMPT_0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkPcRetry("PCRETRY_0");
        detail.setRequestType(ReworkRequestTypeEnum.INVALIDATE_ELEMENTS);
        return detail;
    }

    private Action baseAction(NotificationReworkValidationDetails detail) {
        return Action.builder()
                .actionId("ACTION-INVALIDATE")
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipientIndex(0)
                .details(detail)
                .build();
    }

    private NotificationInt baseNotification() {
        return NotificationInt.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .recipients(List.of(new NotificationRecipientInt()))
                .documents(List.of(NotificationDocumentInt.builder()
                        .ref(NotificationDocumentInt.Ref.builder().key("key").build())
                        .build()))
                .build();
    }

    private void mockBaseValidFlow(NotificationInt notification, Set<TimelineElementInternal> timeline) {
        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.EFFECTIVE_DATE);

        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);
        when(timelineUtils.checkIsNotificationViewed(any(), any())).thenReturn(false);
        when(notificationService.getNotificationByIun(any())).thenReturn(notification);
        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);
        when(timelineService.getTimelineAndStatusHistory(any(), anyInt(), any())).thenReturn(notificationHistoryResponse);
    }

    private Set<TimelineElementInternal> validInvalidateTimeline() {
        Set<TimelineElementInternal> timeline = new HashSet<>();

        timeline.add(timelineElement(
                TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE,
                "PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0",
                BaseAnalogDetailsInt.builder().recIndex(0).sentAttemptMade(0).build()
        ));

        SendAnalogFeedbackDetailsInt feedbackDetails = new SendAnalogFeedbackDetailsInt();
        feedbackDetails.setSentAttemptMade(0);
        feedbackDetails.setResponseStatus(ResponseStatusInt.OK);

        timeline.add(timelineElement(
                TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK,
                "SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0",
                feedbackDetails
        ));

        timeline.add(timelineElement(
                TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK,
                "SEND_ANALOG_PROGRESS.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_1.ATTEMPT_0",
                feedbackDetails
        ));

        timeline.add(timelineElement(
                TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK,
                "SEND_ANALOG_PROGRESS.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0",
                feedbackDetails
        ));

        timeline.add(timelineElement(
                TimelineElementCategoryInt.REFINEMENT,
                "REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                RefinementDetailsInt.builder().recIndex(0).build()
        ));

        return timeline;
    }

    private Set<TimelineElementInternal> validInvalidateTimelineWithoutOkAttempt0() {
        Set<TimelineElementInternal> timeline = new HashSet<>();

        timeline.add(timelineElement(
                TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE,
                "PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0",
                BaseAnalogDetailsInt.builder().recIndex(0).sentAttemptMade(0).build()

        ));

        SendAnalogFeedbackDetailsInt feedbackDetails = new SendAnalogFeedbackDetailsInt();
        feedbackDetails.setSentAttemptMade(0);
        feedbackDetails.setResponseStatus(ResponseStatusInt.KO);

        timeline.add(timelineElement(
                TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK,
                "SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0",
                feedbackDetails
        ));

        timeline.add(timelineElement(
                TimelineElementCategoryInt.REFINEMENT,
                "REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0",
                RefinementDetailsInt.builder().recIndex(0).build()

        ));

        return timeline;
    }

    private TimelineElementInternal timelineElement(
            TimelineElementCategoryInt category,
            String elementId,
            TimelineElementDetailsInt details
    ) {
        TimelineElementInternal element = new TimelineElementInternal();
        element.setCategory(category);
        element.setElementId(elementId);
        element.setTimestamp(Instant.now());
        element.setEventTimestamp(Instant.now());
        element.setNotificationSentAt(Instant.now());
        element.setDetails(details);
        return element;
    }

}

