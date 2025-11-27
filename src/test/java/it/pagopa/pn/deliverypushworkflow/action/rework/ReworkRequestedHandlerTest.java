package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.checkattachmentretention.CheckAttachmentRetentionHandler;
import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkRequestedDetails;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.AddTimelineElementResponse;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.NotificationTimelineReworkedDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogProgressDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.*;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypushworkflow.service.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class ReworkRequestedHandlerTest {

    @Mock
    private TimelineService timelineService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PaperChannelService paperChannelService;
    @Mock
    private SafeStorageService safeStorageService;
    @Mock
    private PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;
    @Mock
    private CheckAttachmentRetentionHandler checkAttachmentRetentionHandler;
    @Mock
    private AttachmentUtils attachmentUtils;

    private ReworkRequestedHandler handler;

    @BeforeEach
    void setup() {
        TimelineUtils timelineUtils = new TimelineUtils(mock(InstantNowSupplier.class), timelineService, mock(NotificationProcessCostService.class));
        handler = new ReworkRequestedHandler(
                timelineService,
                notificationService,
                paperChannelService,
                safeStorageService,
                pnDeliveryPushWorkflowConfigs,
                checkAttachmentRetentionHandler,
                attachmentUtils,
                timelineUtils
        );
    }

    @Test
    void handleNotificationReworkRequestedAttempt0() {
        // Arrange
        NotificationReworkRequestedDetails details = new NotificationReworkRequestedDetails();
        details.setReworkRecIndex("RECINDEX_0");
        details.setReworkAttempt("ATTEMPT_0");
        details.setCreatedAt(Instant.now());
        details.setReworkRequestId("REQID");
        details.setReworkId("REWORK_0_UUID");

        Action action = Action.builder()
                .iun("IUN_2")
                .details(details)
                .build();

        NotificationHistoryResponse historyResponse = new NotificationHistoryResponse();

        List<TimelineElement> timeline = buildTimeline();
        historyResponse.setTimeline(timeline);
        historyResponse.setNotificationStatus(NotificationStatus.DELIVERED);
        List<NotificationStatusHistoryElement> notificationStatusHistory = new ArrayList<>();
        NotificationStatusHistoryElement historyElement = new NotificationStatusHistoryElement();
        historyElement.setStatus(NotificationStatus.DELIVERED);
        historyElement.setRelatedTimelineElements(timeline.stream().map(TimelineElement::getElementId).toList());
        notificationStatusHistory.add(historyElement);

        NotificationStatusHistoryElement historyElement2 = new NotificationStatusHistoryElement();
        historyElement2.setStatus(NotificationStatus.ACCEPTED);
        historyElement2.setRelatedTimelineElements(List.of("REQUEST_ACCEPTED.RECINDEX_0.ATTEMPT_0"));
        notificationStatusHistory.add(historyElement2);

        historyResponse.setNotificationStatusHistory(notificationStatusHistory);
        when(timelineService.getTimelineAndStatusHistory(anyString(), anyInt(), any())).thenReturn(historyResponse);
        when(timelineService.getTimeline(any(), anyBoolean())).thenReturn(buildTimeline().stream().map(t -> {;
            TimelineElementInternal tei = new TimelineElementInternal();
            tei.setCategory(TimelineElementCategoryInt.valueOf(t.getCategory().name()));
            tei.setElementId(t.getElementId());
            tei.setDetails(switch (t.getCategory()) {
                case SEND_ANALOG_PROGRESS -> {
                    SendAnalogProgressDetails details1 = (SendAnalogProgressDetails) t.getDetails();
                    SendAnalogProgressDetailsInt detailsInt = new SendAnalogProgressDetailsInt();
                    detailsInt.setDeliveryDetailCode(details1.getDeliveryDetailCode());
                    yield detailsInt;
                }
                default -> null;
            });
            return tei;
        }).collect(java.util.stream.Collectors.toSet()));

        NotificationInt notification = NotificationInt.builder()
                .sentAt(Instant.now())
                .recipients(List.of(new NotificationRecipientInt()))
                .iun("IUN_2")
                .sender(NotificationSenderInt.builder().paId("paId").build())
                .documents(Collections.emptyList())
                .build();
        when(notificationService.getNotificationByIun(anyString())).thenReturn(notification);
        ArgumentCaptor<TimelineElementInternal> argumentCaptor = ArgumentCaptor.forClass(TimelineElementInternal.class);
        when(timelineService.addTimelineElement(any(), any())).thenReturn((new AddTimelineElementResponse(null, true)));

        when(pnDeliveryPushWorkflowConfigs.getTimeParams()).thenReturn(mock(TimeParams.class));
        when(pnDeliveryPushWorkflowConfigs.getTimeParams().getAttachmentTimeToAddAfterExpiration()).thenReturn(java.time.Duration.ofDays(30));
        when(pnDeliveryPushWorkflowConfigs.getInvalidableCategories()).thenReturn(List.of("PREPARE_ANALOG_DOMICILE","PREPARE_ANALOG_DOMICILE_FAILURE","SEND_ANALOG_DOMICILE","SEND_ANALOG_PROGRESS","SEND_ANALOG_FEEDBACK","ANALOG_SUCCESS_WORKFLOW","ANALOG_FAILURE_WORKFLOW","SCHEDULE_REFINEMENT","REFINEMENT","COMPLETELY_UNREACHABLE_CREATION_REQUEST","COMPLETELY_UNREACHABLE","ANALOG_WORKFLOW_RECIPIENT_DECEASED"));

        // Act & Assert
        handler.handleNotificationReworkRequested(action).block();

        verify(timelineService).addTimelineElement(argumentCaptor.capture(), eq(notification));
        TimelineElementInternal capturedElement = argumentCaptor.getValue();
        Assertions.assertEquals("NOTIFICATION_TIMELINE_REWORKED", capturedElement.getCategory().name());
        Assertions.assertEquals("NOTIFICATION_TIMELINE_REWORKED.IUN_IUN_2.RECINDEX_0.ATTEMPT_0.REWORK_0", capturedElement.getElementId());
        NotificationTimelineReworkedDetailsInt detailsInt = (NotificationTimelineReworkedDetailsInt) capturedElement.getDetails();
        Assertions.assertEquals(1, detailsInt.getInvalidatedTimelineAndStatusHistory().size());
        Assertions.assertEquals(7, detailsInt.getInvalidatedTimelineAndStatusHistory().getFirst().getRelatedTimelineElements().size());

    }

    @Test
    void handleNotificationReworkRequestedAttempt1() {
        // Arrange
        NotificationReworkRequestedDetails details = new NotificationReworkRequestedDetails();
        details.setReworkRecIndex("RECINDEX_0");
        details.setReworkAttempt("ATTEMPT_1");
        details.setCreatedAt(Instant.now());
        details.setReworkRequestId("REQID");
        details.setReworkId("REWORK_0_UUID");

        Action action = Action.builder()
                .iun("IUN_2")
                .details(details)
                .build();

        NotificationHistoryResponse historyResponse = new NotificationHistoryResponse();

        List<TimelineElement> timeline = buildTimeline();
        historyResponse.setTimeline(timeline);
        historyResponse.setNotificationStatus(NotificationStatus.DELIVERED);
        List<NotificationStatusHistoryElement> notificationStatusHistory = new ArrayList<>();
        NotificationStatusHistoryElement historyElement = new NotificationStatusHistoryElement();
        historyElement.setStatus(NotificationStatus.DELIVERED);
        historyElement.setRelatedTimelineElements(timeline.stream().map(TimelineElement::getElementId).toList());
        notificationStatusHistory.add(historyElement);
        historyResponse.setNotificationStatusHistory(notificationStatusHistory);
        when(timelineService.getTimelineAndStatusHistory(anyString(), anyInt(), any())).thenReturn(historyResponse);
        when(timelineService.getTimeline(any(), anyBoolean())).thenReturn(buildTimeline().stream().map(t -> {;
            TimelineElementInternal tei = new TimelineElementInternal();
            tei.setCategory(TimelineElementCategoryInt.valueOf(t.getCategory().name()));
            tei.setElementId(t.getElementId());
            tei.setDetails(switch (t.getCategory()) {
                case SEND_ANALOG_PROGRESS -> {
                    SendAnalogProgressDetails details1 = (SendAnalogProgressDetails) t.getDetails();
                    SendAnalogProgressDetailsInt detailsInt = new SendAnalogProgressDetailsInt();
                    detailsInt.setDeliveryDetailCode(details1.getDeliveryDetailCode());
                    yield detailsInt;
                }
                default -> null;
            });
            return tei;
        }).collect(java.util.stream.Collectors.toSet()));

        NotificationInt notification = NotificationInt.builder()
                .sentAt(Instant.now())
                .recipients(List.of(new NotificationRecipientInt()))
                .iun("IUN_2")
                .sender(NotificationSenderInt.builder().paId("paId").build())
                .documents(Collections.emptyList())
                .build();
        when(notificationService.getNotificationByIun(anyString())).thenReturn(notification);
        ArgumentCaptor<TimelineElementInternal> argumentCaptor = ArgumentCaptor.forClass(TimelineElementInternal.class);
        when(timelineService.addTimelineElement(any(), any())).thenReturn(new AddTimelineElementResponse(null, true));

        when(pnDeliveryPushWorkflowConfigs.getTimeParams()).thenReturn(mock(TimeParams.class));
        when(pnDeliveryPushWorkflowConfigs.getTimeParams().getAttachmentTimeToAddAfterExpiration()).thenReturn(java.time.Duration.ofDays(30));
        when(pnDeliveryPushWorkflowConfigs.getInvalidableCategories()).thenReturn(List.of("PREPARE_ANALOG_DOMICILE","PREPARE_ANALOG_DOMICILE_FAILURE","SEND_ANALOG_DOMICILE","SEND_ANALOG_PROGRESS","SEND_ANALOG_FEEDBACK","ANALOG_SUCCESS_WORKFLOW","ANALOG_FAILURE_WORKFLOW","SCHEDULE_REFINEMENT","REFINEMENT","COMPLETELY_UNREACHABLE_CREATION_REQUEST","COMPLETELY_UNREACHABLE","ANALOG_WORKFLOW_RECIPIENT_DECEASED"));

        // Act & Assert
        handler.handleNotificationReworkRequested(action).block();

        verify(timelineService).addTimelineElement(argumentCaptor.capture(), eq(notification));
        TimelineElementInternal capturedElement = argumentCaptor.getValue();
        Assertions.assertEquals("NOTIFICATION_TIMELINE_REWORKED", capturedElement.getCategory().name());
        Assertions.assertEquals("NOTIFICATION_TIMELINE_REWORKED.IUN_IUN_2.RECINDEX_0.ATTEMPT_1.REWORK_0", capturedElement.getElementId());
        NotificationTimelineReworkedDetailsInt detailsInt = (NotificationTimelineReworkedDetailsInt) capturedElement.getDetails();
        Assertions.assertEquals(1, detailsInt.getInvalidatedTimelineAndStatusHistory().size());
        Assertions.assertEquals(3, detailsInt.getInvalidatedTimelineAndStatusHistory().getFirst().getRelatedTimelineElements().size());

    }

    private List<TimelineElement> buildTimeline() {
        List<TimelineElement> timeline = new ArrayList<>();
        TimelineElement timelineElement = new TimelineElement();
        timelineElement.elementId("SEND_ANALOG_FEEDBACK.RECINDEX_0.ATTEMPT_0");
        timelineElement.category(TimelineCategory.SEND_ANALOG_FEEDBACK);
        timeline.add(timelineElement);

        timelineElement = new TimelineElement();
        timelineElement.elementId("SEND_ANALOG_DOMICILE.RECINDEX_0.ATTEMPT_1");
        timelineElement.category(TimelineCategory.SEND_ANALOG_DOMICILE);
        timeline.add(timelineElement);

        timelineElement = new TimelineElement();
        timelineElement.elementId("SEND_ANALOG_DOMICILE.RECINDEX_0.ATTEMPT_0");
        timelineElement.category(TimelineCategory.SEND_ANALOG_DOMICILE);
        timeline.add(timelineElement);

        timelineElement = new TimelineElement();
        timelineElement.elementId("PREPARE_ANALOG_DOMICILE.RECINDEX_0.ATTEMPT_0");
        timelineElement.category(TimelineCategory.PREPARE_ANALOG_DOMICILE);

        timeline.add(timelineElement);

        timelineElement = new TimelineElement();
        timelineElement.elementId("GET_ADDRESS.RECINDEX_0.ATTEMPT_0");
        timelineElement.category(TimelineCategory.PAYMENT);

        timeline.add(timelineElement);

        SendAnalogProgressDetails detail = new SendAnalogProgressDetails();
        detail.setDeliveryDetailCode("RECRN");
        timelineElement = new TimelineElement();
        timelineElement.elementId("SEND_ANALOG_PROGRESS.RECINDEX_0.ATTEMPT_0.IDX_0");
        timelineElement.category(TimelineCategory.SEND_ANALOG_PROGRESS);
        timelineElement.details(detail);

        timeline.add(timelineElement);

        timelineElement = new TimelineElement();
        timelineElement.elementId("SEND_ANALOG_FEEDBACK.RECINDEX_0.ATTEMPT_1");
        timelineElement.category(TimelineCategory.SEND_ANALOG_FEEDBACK);
        timeline.add(timelineElement);

        timelineElement = new TimelineElement();
        timelineElement.elementId("PREPARE_ANALOG_DOMICILE.RECINDEX_0.ATTEMPT_1");
        timelineElement.category(TimelineCategory.PREPARE_ANALOG_DOMICILE);
        timeline.add(timelineElement);

        timelineElement = new TimelineElement();
        timelineElement.elementId("REFINEMENT.RECINDEX_0");
        timelineElement.category(TimelineCategory.REFINEMENT);
        timeline.add(timelineElement);

        timelineElement = new TimelineElement();
        timelineElement.elementId("REFINEMENT.RECINDEX_1");
        timelineElement.category(TimelineCategory.REFINEMENT);
        timeline.add(timelineElement);

        detail = new SendAnalogProgressDetails();
        detail.setDeliveryDetailCode("RECRN");
        timelineElement = new TimelineElement();
        timelineElement.elementId("SEND_ANALOG_PROGRESS.RECINDEX_0.ATTEMPT_1.IDX_0");
        timelineElement.category(TimelineCategory.SEND_ANALOG_PROGRESS);
        timelineElement.details(detail);
        timeline.add(timelineElement);

        detail = new SendAnalogProgressDetails();
        detail.setDeliveryDetailCode("CON996");
        timelineElement = new TimelineElement();
        timelineElement.elementId("SEND_ANALOG_PROGRESS.RECINDEX_0.ATTEMPT_0.IDX_2");
        timelineElement.category(TimelineCategory.SEND_ANALOG_PROGRESS);
        timelineElement.details(detail);
        timeline.add(timelineElement);

        return timeline;
    }
}