package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.checkattachmentretention.CheckAttachmentRetentionHandler;
import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkRequestedDetails;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogProgressDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.PaperChannelService;
import it.pagopa.pn.deliverypushworkflow.service.SafeStorageService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
        handler = new ReworkRequestedHandler(
                timelineService,
                notificationService,
                paperChannelService,
                safeStorageService,
                pnDeliveryPushWorkflowConfigs,
                checkAttachmentRetentionHandler,
                attachmentUtils
        );
    }

    @Test
    void handleNotificationReworkRequested_OK() {
        // Arrange
        NotificationReworkRequestedDetails details = new NotificationReworkRequestedDetails();
        details.setRecIndex("RECINDEX_0");
        details.setAttempt("ATTEMPT_0");
        details.setCreatedAt(Instant.now());
        details.setReworkrequestId("REQID");
        details.setReworkId("REWID");

        Action action = Action.builder()
                .iun("IUN_1")
                .details(details)
                .build();

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(buildTimeline());

        NotificationInt notification = NotificationInt.builder()
                .iun("IUN_1")
                .documents(List.of(NotificationDocumentInt.builder().ref(NotificationDocumentInt.Ref.builder().key("fileKey").build()).build()))
                .build();
        when(notificationService.getNotificationByIun(anyString())).thenReturn(notification);

        when(paperChannelService.initNotificationRework(anyString(), anyString())).thenReturn("OK");

        when(pnDeliveryPushWorkflowConfigs.getTimeParams()).thenReturn(mock(TimeParams.class));
        when(pnDeliveryPushWorkflowConfigs.getTimeParams().getAttachmentTimeToAddAfterExpiration()).thenReturn(java.time.Duration.ofDays(30));
        when(pnDeliveryPushWorkflowConfigs.getInvalidableCategories()).thenReturn(List.of("PREPARE_ANALOG_DOMICILE","PREPARE_ANALOG_DOMICILE_FAILURE","SEND_ANALOG_DOMICILE","SEND_ANALOG_PROGRESS","SEND_ANALOG_FEEDBACK","ANALOG_SUCCESS_WORKFLOW","ANALOG_FAILURE_WORKFLOW","SCHEDULE_REFINEMENT","REFINEMENT","COMPLETELY_UNREACHABLE_CREATION_REQUEST","COMPLETELY_UNREACHABLE","ANALOG_WORKFLOW_RECIPIENT_DECEASED"));

        FileDownloadResponse response = new FileDownloadResponse();
        response.key("fileKey");
        response.retentionUntil(OffsetDateTime.now().plusDays(10));

        when(safeStorageService.getFile(anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(Mono.just(response));

        when(attachmentUtils.changeAttachmentRetention(anyString(), any())).thenReturn(Mono.just("fileKey"));

        // Act & Assert
        assertDoesNotThrow(() -> handler.handleNotificationReworkRequested(action).block());
        verify(checkAttachmentRetentionHandler, atLeastOnce()).scheduleCheckAttachmentRetentionBeforeExpiration(anyString(), any());
    }

    @Test
    void handleNotificationReworkRequested_OK_ATTEMPT1() {
        // Arrange
        NotificationReworkRequestedDetails details = new NotificationReworkRequestedDetails();
        details.setRecIndex("RECINDEX_0");
        details.setAttempt("ATTEMPT_1");
        details.setCreatedAt(Instant.now());
        details.setReworkrequestId("REQID");
        details.setReworkId("REWID");

        Action action = Action.builder()
                .iun("IUN_1")
                .details(details)
                .build();

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(buildTimeline());

        NotificationInt notification = NotificationInt.builder()
                .iun("IUN_1")
                .documents(List.of(NotificationDocumentInt.builder().ref(NotificationDocumentInt.Ref.builder().key("fileKey").build()).build()))
                .build();
        when(notificationService.getNotificationByIun(anyString())).thenReturn(notification);

        when(paperChannelService.initNotificationRework(anyString(), anyString())).thenReturn("OK");

        when(pnDeliveryPushWorkflowConfigs.getTimeParams()).thenReturn(mock(TimeParams.class));
        when(pnDeliveryPushWorkflowConfigs.getTimeParams().getAttachmentTimeToAddAfterExpiration()).thenReturn(java.time.Duration.ofDays(30));
        when(pnDeliveryPushWorkflowConfigs.getInvalidableCategories()).thenReturn(List.of("PREPARE_ANALOG_DOMICILE","PREPARE_ANALOG_DOMICILE_FAILURE","SEND_ANALOG_DOMICILE","SEND_ANALOG_PROGRESS","SEND_ANALOG_FEEDBACK","ANALOG_SUCCESS_WORKFLOW","ANALOG_FAILURE_WORKFLOW","SCHEDULE_REFINEMENT","REFINEMENT","COMPLETELY_UNREACHABLE_CREATION_REQUEST","COMPLETELY_UNREACHABLE","ANALOG_WORKFLOW_RECIPIENT_DECEASED"));

        FileDownloadResponse response = new FileDownloadResponse();
        response.key("fileKey");
        response.retentionUntil(OffsetDateTime.now().plusDays(10));

        when(safeStorageService.getFile(anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(Mono.just(response));

        when(attachmentUtils.changeAttachmentRetention(anyString(), any())).thenReturn(Mono.just("fileKey"));

        // Act & Assert
        assertDoesNotThrow(() -> handler.handleNotificationReworkRequested(action).block());
        verify(checkAttachmentRetentionHandler, atLeastOnce()).scheduleCheckAttachmentRetentionBeforeExpiration(anyString(), any());
    }

    @Test
    void handleNotificationReworkRequested_NoInvalidableElements() {
        // Arrange
        NotificationReworkRequestedDetails details = new NotificationReworkRequestedDetails();
        details.setRecIndex("RECINDEX_0");
        details.setAttempt("ATTEMPT_0");
        details.setCreatedAt(Instant.now());
        details.setReworkrequestId("REQID");
        details.setReworkId("REWID");

        Action action = Action.builder()
                .iun("IUN_2")
                .details(details)
                .build();

        // No elements matching invalidable categories
        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(buildTimelineNoInvalidableElements());

        NotificationInt notification = NotificationInt.builder()
                .iun("IUN_2")
                .documents(Collections.emptyList())
                .build();
        when(notificationService.getNotificationByIun(anyString())).thenReturn(notification);

        when(paperChannelService.initNotificationRework(anyString(), anyString())).thenReturn("OK");
        when(pnDeliveryPushWorkflowConfigs.getTimeParams()).thenReturn(mock(TimeParams.class));
        when(pnDeliveryPushWorkflowConfigs.getTimeParams().getAttachmentTimeToAddAfterExpiration()).thenReturn(java.time.Duration.ofDays(30));
        when(pnDeliveryPushWorkflowConfigs.getInvalidableCategories()).thenReturn(List.of("PREPARE_ANALOG_DOMICILE","PREPARE_ANALOG_DOMICILE_FAILURE","SEND_ANALOG_DOMICILE","SEND_ANALOG_PROGRESS","SEND_ANALOG_FEEDBACK","ANALOG_SUCCESS_WORKFLOW","ANALOG_FAILURE_WORKFLOW","SCHEDULE_REFINEMENT","REFINEMENT","COMPLETELY_UNREACHABLE_CREATION_REQUEST","COMPLETELY_UNREACHABLE","ANALOG_WORKFLOW_RECIPIENT_DECEASED"));

        // Act & Assert
        assertDoesNotThrow(() -> handler.handleNotificationReworkRequested(action).block());
    }

    @Test
    void handleNotificationReworkRequested_AttachmentRetentionUpdate() {
        // Arrange
        NotificationReworkRequestedDetails details = new NotificationReworkRequestedDetails();
        details.setRecIndex("RECINDEX_0");
        details.setAttempt("ATTEMPT_0");
        details.setCreatedAt(Instant.now());
        details.setReworkrequestId("REQID");
        details.setReworkId("REWID");

        Action action = Action.builder()
                .iun("IUN_3")
                .details(details)
                .build();

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(buildTimeline());

        NotificationDocumentInt doc = NotificationDocumentInt.builder()
                .ref(NotificationDocumentInt.Ref.builder().key("fileKey").build())
                .build();
        NotificationInt notification = NotificationInt.builder()
                .iun("IUN_3")
                .documents(List.of(doc))
                .build();
        when(notificationService.getNotificationByIun(anyString())).thenReturn(notification);

        when(paperChannelService.initNotificationRework(anyString(), anyString())).thenReturn("OK");
        when(pnDeliveryPushWorkflowConfigs.getTimeParams()).thenReturn(mock(TimeParams.class));
        when(pnDeliveryPushWorkflowConfigs.getTimeParams().getAttachmentTimeToAddAfterExpiration()).thenReturn(java.time.Duration.ofDays(30));
        when(pnDeliveryPushWorkflowConfigs.getInvalidableCategories()).thenReturn(List.of("PREPARE_ANALOG_DOMICILE","PREPARE_ANALOG_DOMICILE_FAILURE","SEND_ANALOG_DOMICILE","SEND_ANALOG_PROGRESS","SEND_ANALOG_FEEDBACK","ANALOG_SUCCESS_WORKFLOW","ANALOG_FAILURE_WORKFLOW","SCHEDULE_REFINEMENT","REFINEMENT","COMPLETELY_UNREACHABLE_CREATION_REQUEST","COMPLETELY_UNREACHABLE","ANALOG_WORKFLOW_RECIPIENT_DECEASED"));

        OffsetDateTime oldRetention = OffsetDateTime.now().minusDays(1);

        FileDownloadResponse response = new FileDownloadResponse();
        response.key("fileKey");
        response.retentionUntil(oldRetention);

        when(safeStorageService.getFile(anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(Mono.just(response));

        when(attachmentUtils.changeAttachmentRetention(anyString(), any())).thenReturn(Mono.just("fileKey"));

        // Act
        handler.handleNotificationReworkRequested(action).block();

        // Assert
        verify(attachmentUtils, atLeastOnce()).changeAttachmentRetention(eq("fileKey"), any());
        verify(checkAttachmentRetentionHandler, atLeastOnce()).scheduleCheckAttachmentRetentionBeforeExpiration(anyString(), any());
    }

    private Set<TimelineElementInternal> buildTimeline() {
        Set<TimelineElementInternal> timeline = new HashSet<>();
        TimelineElementInternal timelineElement = TimelineElementInternal.builder()
                .elementId("RECINDEX_0.ATTEMPT_0")
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .build();
        timeline.add(timelineElement);

        timelineElement = TimelineElementInternal.builder()
                .elementId("RECINDEX_0.ATTEMPT_0")
                .category(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE)
                .build();
        timeline.add(timelineElement);

        timelineElement = TimelineElementInternal.builder()
                .elementId("RECINDEX_0.ATTEMPT_0")
                .category(TimelineElementCategoryInt.PAYMENT)
                .build();
        timeline.add(timelineElement);

        SendAnalogProgressDetailsInt detail = new SendAnalogProgressDetailsInt();
        detail.setDeliveryDetailCode("RECRN");
        timelineElement = TimelineElementInternal.builder()
                .elementId("RECINDEX_0.ATTEMPT_0")
                .category(TimelineElementCategoryInt.SEND_ANALOG_PROGRESS)
                .details(detail)
                .build();
        timeline.add(timelineElement);

        timelineElement = TimelineElementInternal.builder()
                .elementId("RECINDEX_0.ATTEMPT_1")
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .build();
        timeline.add(timelineElement);

        timelineElement = TimelineElementInternal.builder()
                .elementId("RECINDEX_0.ATTEMPT_1")
                .category(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE)
                .build();
        timeline.add(timelineElement);

        timelineElement = TimelineElementInternal.builder()
                .elementId("RECINDEX_0.ATTEMPT_1")
                .category(TimelineElementCategoryInt.PAYMENT)
                .build();
        timeline.add(timelineElement);

        detail = new SendAnalogProgressDetailsInt();
        detail.setDeliveryDetailCode("RECRN");
        timelineElement = TimelineElementInternal.builder()
                .elementId("RECINDEX_0.ATTEMPT_1")
                .category(TimelineElementCategoryInt.SEND_ANALOG_PROGRESS)
                .details(detail)
                .build();
        timeline.add(timelineElement);

        return timeline;
    }

    private Set<TimelineElementInternal> buildTimelineNoInvalidableElements() {
        Set<TimelineElementInternal> timeline = new HashSet<>();

        TimelineElementInternal timelineElement = TimelineElementInternal.builder()
                .elementId("RECINDEX_0.ATTEMPT_0")
                .category(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE)
                .build();
        timeline.add(timelineElement);

        SendAnalogProgressDetailsInt detail = new SendAnalogProgressDetailsInt();
        detail.setDeliveryDetailCode("CON");
        timelineElement = TimelineElementInternal.builder()
                .elementId("RECINDEX_0.ATTEMPT_0")
                .category(TimelineElementCategoryInt.SEND_ANALOG_PROGRESS)
                .details(detail)
                .build();
        timeline.add(timelineElement);

        timelineElement = TimelineElementInternal.builder()
                .elementId("RECINDEX_0.ATTEMPT_1")
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .build();
        timeline.add(timelineElement);

        timelineElement = TimelineElementInternal.builder()
                .elementId("RECINDEX_0.ATTEMPT_1")
                .category(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE)
                .build();
        timeline.add(timelineElement);


        detail = new SendAnalogProgressDetailsInt();
        detail.setDeliveryDetailCode("CON");
        timelineElement = TimelineElementInternal.builder()
                .elementId("RECINDEX_0.ATTEMPT_1")
                .category(TimelineElementCategoryInt.SEND_ANALOG_PROGRESS)
                .details(detail)
                .build();
        timeline.add(timelineElement);

        return timeline;
    }
}