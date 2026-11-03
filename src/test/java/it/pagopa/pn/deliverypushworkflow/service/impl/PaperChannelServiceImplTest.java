package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypushworkflow.action.analogworkflow.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation.F24ResolutionMode;
import it.pagopa.pn.deliverypushworkflow.action.utils.AnalogDeliveryTimeoutUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.PaperChannelUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypushworkflow.dto.ext.paperchannel.AnalogDtoInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.paperchannel.CategorizedAttachmentsResultInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.paperchannel.ResultFilterInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.ReceiverAddressSourceTypeInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.model.ResultFilterEnum;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.model.SendResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.paperchannel.PaperChannelPrepareRequest;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.paperchannel.PaperChannelSendClient;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypushworkflow.service.AuditLogService;
import it.pagopa.pn.deliverypushworkflow.service.PaperChannelService;
import it.pagopa.pn.deliverypushworkflow.service.SchedulerService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;

class PaperChannelServiceImplTest {
    @Mock
    private PaperChannelUtils paperChannelUtils;
    @Mock
    private PaperChannelSendClient paperChannelSendClient;
    @Mock
    private NotificationUtils notificationUtils;

    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private AnalogWorkflowUtils analogWorkflowUtils;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private PnDeliveryPushWorkflowConfigs pnDeliveryPushConfigs;
    @Mock
    private TimelineService timelineService;

    private PaperChannelService paperChannelService;
    @Mock
    private AnalogDeliveryTimeoutUtils analogDeliveryTimeoutUtils;


    @Mock
    private AttachmentUtils attachmentUtils;

    @BeforeEach
    void setup() {
        paperChannelService = new PaperChannelServiceImpl(
                paperChannelUtils,
                paperChannelSendClient,
                notificationUtils,
                timelineUtils,
                analogWorkflowUtils,
                auditLogService,
                attachmentUtils,
                schedulerService,
                pnDeliveryPushConfigs,
                timelineService,
                analogDeliveryTimeoutUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @ParameterizedTest
    @EnumSource(value = ReceiverAddressSourceTypeInt.class, names = {"MITTENTE", "ANPR", "REGISTRO_IMPRESE"})
    void prepareAnalogNotificationForSimpleRegisteredLetterWithAarAndDocument(
            ReceiverAddressSourceTypeInt source) {
        //GIVEN
        NotificationInt notificationInt = newNotificationWithPayments();
        PhysicalAddressInt receiverAddress = PhysicalAddressInt.builder().address("Via Roma 1").build();
        Mockito.when(analogWorkflowUtils.getPhysicalAddress(notificationInt, 0)).thenReturn(receiverAddress);
        Mockito.when(paperChannelUtils.resolveAddressSource(notificationInt.getIun(), 0)).thenReturn(source);
        Mockito.when(paperChannelUtils.buildPrepareSimpleRegisteredLetterEventId(notificationInt, 0))
                .thenReturn("prepare-simple");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().getFirst());

        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        ArgumentCaptor<PaperChannelPrepareRequest> captor =
                ArgumentCaptor.forClass(PaperChannelPrepareRequest.class);
        Mockito.verify(paperChannelSendClient).prepare(captor.capture());
        PaperChannelPrepareRequest request = captor.getValue();
        Assertions.assertAll(
                () -> Assertions.assertEquals(source, request.getReceiverAddressSourceType()),
                () -> Assertions.assertEquals("prepare-simple", request.getRequestId()),
                () -> Assertions.assertSame(receiverAddress, request.getPaAddress()),
                () -> Assertions.assertSame(notificationInt, request.getNotificationInt()),
                () -> Assertions.assertSame(notificationInt.getRecipients().getFirst(), request.getRecipientInt()),
                () -> Assertions.assertEquals(PhysicalAddressInt.ANALOG_TYPE.SIMPLE_REGISTERED_LETTER, request.getAnalogType()),
                () -> Assertions.assertNull(request.getRelatedRequestId()),
                () -> Assertions.assertNull(request.getDiscoveredAddress())
        );
        Mockito.verify(paperChannelUtils).resolveAddressSource(notificationInt.getIun(), 0);
        Mockito.verify(paperChannelUtils).addPrepareSimpleRegisteredLetterToTimeline(
                notificationInt, receiverAddress, 0, "prepare-simple");
        Mockito.verify(attachmentUtils).retrieveAttachments(any(),any(),eq(SendAttachmentMode.AAR_DOCUMENTS),eq(F24ResolutionMode.URL),any(),any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationForSimpleRegisteredLetterWithAarDocumentAndPayments() {
        //GIVEN
        NotificationInt notificationInt = newNotificationWithPayments();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().getFirst());


        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(any());
        Mockito.verify(attachmentUtils).retrieveAttachments(any(), any(), eq(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS), eq(F24ResolutionMode.URL), any(), any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationWithAarAndDocument() {
        //GIVEN
        NotificationInt notificationInt = newNotificationWithPayments();
        PhysicalAddressInt receiverAddress = PhysicalAddressInt.builder().address("Via Roma 1").build();
        Mockito.when(analogWorkflowUtils.getPhysicalAddress(notificationInt, 0)).thenReturn(receiverAddress);
        Mockito.when(paperChannelUtils.resolveAddressSource(notificationInt.getIun(), 0))
                .thenReturn(ReceiverAddressSourceTypeInt.ANPR);

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().getFirst());

        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS);

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 0);

        // THEN
        ArgumentCaptor<PaperChannelPrepareRequest> requestCaptor =
                ArgumentCaptor.forClass(PaperChannelPrepareRequest.class);
        Mockito.verify(paperChannelSendClient).prepare(requestCaptor.capture());
        PaperChannelPrepareRequest request = requestCaptor.getValue();
        Assertions.assertAll(
                () -> Assertions.assertEquals(ReceiverAddressSourceTypeInt.ANPR, request.getReceiverAddressSourceType()),
                () -> Assertions.assertSame(receiverAddress, request.getPaAddress()),
                () -> Assertions.assertSame(notificationInt, request.getNotificationInt()),
                () -> Assertions.assertSame(notificationInt.getRecipients().getFirst(), request.getRecipientInt()),
                () -> Assertions.assertNull(request.getRelatedRequestId()),
                () -> Assertions.assertNull(request.getDiscoveredAddress())
        );
        Mockito.verify(attachmentUtils).retrieveAttachments(any(),any(),eq(SendAttachmentMode.AAR_DOCUMENTS), eq(F24ResolutionMode.URL),any(),any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationWithAarDocumentAndPayments() {
        //GIVEN
        NotificationInt notificationInt = newNotificationWithPayments();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().getFirst());

        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS);

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(any());
        Mockito.verify(attachmentUtils).retrieveAttachments(any(), any(), eq(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS), eq(F24ResolutionMode.URL),any(),any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationForSimpleRegisteredLetterAlreadyviewed() {
        //GIVEN
        NotificationInt notificationInt = newNotification();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).prepare(any());
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationForSimpleRegisteredLetterWhenPaid() {
        //GIVEN
        NotificationInt notificationInt = newNotification();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.lenient().when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(Mockito.any());
        Mockito.verify(paperChannelUtils).addPrepareSimpleRegisteredLetterToTimeline(any(), any(), eq(0), any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationForSimpleRegisteredLetterCancelled() {
        //GIVEN
        NotificationInt notificationInt = newNotification();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(true);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient,Mockito.never()).prepare(Mockito.any());
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationAlreadyviewed() {
        //GIVEN
        NotificationInt notificationInt = newNotification();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 0);

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).prepare(Mockito.any());
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationWhenPaid() {
        //GIVEN
        NotificationInt notificationInt = newNotification();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.lenient().when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(Mockito.any());
        Mockito.verify(paperChannelUtils).addPrepareAnalogNotificationToTimeline(any(), any(), eq(0), any(), eq(0), any(), any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationCancelled() {
        //GIVEN
        NotificationInt notificationInt = newNotification();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(true);

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 0);

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).prepare(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @ParameterizedTest
    @NullSource
    @EnumSource(value = ReceiverAddressSourceTypeInt.class, names = {"MITTENTE", "ANPR", "REGISTRO_IMPRESE"})
    void sendSimpleRegisteredLetter(ReceiverAddressSourceTypeInt source) {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotificationWithPayments();

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(any(), (Integer) any(), any())).thenReturn(auditLogEvent);
        SendResponse sendResponse = new SendResponse().amount(125);
        Mockito.when(paperChannelSendClient.send(Mockito.any(PaperChannelSendRequest.class))).thenReturn(sendResponse);
        Mockito.when(paperChannelUtils.addSendSimpleRegisteredLetterToTimeline(
                notificationInt, physicalAddressInt, 0, sendResponse, "NR_SR", "req123",
                List.of("replacedF24Url"), categorizedAttachmentsResult, source)).thenReturn("send-simple");


        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS);

        // WHEN
        String timelineId = paperChannelService.sendSimpleRegisteredLetter(
                notificationInt, 0, "req123", physicalAddressInt, "NR_SR",
                List.of("replacedF24Url"), categorizedAttachmentsResult, source);

        // THEN
        Mockito.verify(paperChannelSendClient).send(any());
        Assertions.assertEquals("send-simple", timelineId);
        Mockito.verify(paperChannelUtils).addSendSimpleRegisteredLetterToTimeline(
                notificationInt, physicalAddressInt, 0, sendResponse, "NR_SR", "req123",
                List.of("replacedF24Url"), categorizedAttachmentsResult, source);
        Mockito.verify(paperChannelUtils, Mockito.never()).resolveAddressSource(anyString(), anyInt());
        Mockito.verify(auditLogEvent).generateSuccess(any(), (Integer) any(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(any());
        Mockito.verify(attachmentUtils).retrieveAttachments(any(), any(), eq(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS), eq(F24ResolutionMode.RESOLVE_WITH_REPLACED_LIST), any(),any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendSimpleRegisteredLetterAlreadyViewed() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification();

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult, ReceiverAddressSourceTypeInt.MITTENTE);

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).send(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendSimpleRegisteredLetterCancelled() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification();

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(true);

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult, ReceiverAddressSourceTypeInt.MITTENTE);

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).send(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendSimpleRegisteredLetterWhenPaid() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification();

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.lenient().when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(any(), (Integer) any(), any())).thenReturn(auditLogEvent);
        Mockito.when(paperChannelSendClient.send(Mockito.any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult, ReceiverAddressSourceTypeInt.MITTENTE);

        // THEN
        Mockito.verify(paperChannelSendClient).send(any());
        Mockito.verify(paperChannelUtils).addSendSimpleRegisteredLetterToTimeline(any(), any(), eq(0), any(), any(), eq("req123"), any(), any(), eq(ReceiverAddressSourceTypeInt.MITTENTE));
        Mockito.verify(auditLogEvent).generateSuccess(any(), (Integer) any(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotification() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification();

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);


        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(any(), (Integer) any(), any())).thenReturn(auditLogEvent);

        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());


        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS);

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult, ReceiverAddressSourceTypeInt.MITTENTE);

        // THEN
        Mockito.verify(paperChannelSendClient).send(any());
        ArgumentCaptor<AnalogDtoInt> analogDtoCaptor = ArgumentCaptor.forClass(AnalogDtoInt.class);
        Mockito.verify(paperChannelUtils).addSendAnalogNotificationToTimeline(
                eq(notificationInt), same(physicalAddressInt), eq(0), analogDtoCaptor.capture(),
                eq(List.of("replacedF24Url")), same(categorizedAttachmentsResult), eq("req123"));
        AnalogDtoInt analogDto = analogDtoCaptor.getValue();
        Assertions.assertAll(
                () -> Assertions.assertEquals(ReceiverAddressSourceTypeInt.MITTENTE, analogDto.getReceiverAddressSourceType()),
                () -> Assertions.assertNull(analogDto.getHasOnSiteInvestigation()),
                () -> Assertions.assertEquals(0, analogDto.getSentAttemptMade()),
                () -> Assertions.assertNull(analogDto.getRelatedRequestId()),
                () -> Assertions.assertEquals("req123", analogDto.getPrepareRequestId()),
                () -> Assertions.assertEquals("NR_SR", analogDto.getProductType()),
                () -> Assertions.assertNotNull(analogDto.getSendResponse())
        );
        Mockito.verifyNoInteractions(analogDeliveryTimeoutUtils);
        Mockito.verify(auditLogEvent).generateSuccess(any(), (Integer) any(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotificationAlreadyViewed() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification();

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult, ReceiverAddressSourceTypeInt.MITTENTE);

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).send(any());
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotificationWhenPaid() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification();

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.lenient().when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(any(), (Integer) any(), any())).thenReturn(auditLogEvent);
        Mockito.when(paperChannelSendClient.send(Mockito.any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult, ReceiverAddressSourceTypeInt.MITTENTE);

        // THEN
        Mockito.verify(paperChannelSendClient).send(any());
        Mockito.verify(paperChannelUtils).addSendAnalogNotificationToTimeline(any(), any(), eq(0), any(), any(), any(), eq("req123"));
        Mockito.verify(auditLogEvent).generateSuccess(any(), (Integer) any(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotificationCancelled() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification();

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(true);

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult, ReceiverAddressSourceTypeInt.MITTENTE);

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).send(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationAttachments_CfgNone() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification();

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(any(), (Integer) any(), any())).thenReturn(auditLogEvent);
        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());
        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR);
        
        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR", Collections.emptyList(), categorizedAttachmentsResult, ReceiverAddressSourceTypeInt.MITTENTE);

        // THEN
        Mockito.verify(attachmentUtils).retrieveAttachments(any(), any(), eq(SendAttachmentMode.AAR), eq(F24ResolutionMode.RESOLVE_WITH_REPLACED_LIST), any(), any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationAttachments_CfgSimpleRegisteredLetter() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotificationWithPayments();

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().getFirst());
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(any(), (Integer) any(), any())).thenReturn(auditLogEvent);
        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS);

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult, ReceiverAddressSourceTypeInt.MITTENTE);

        // THEN
        Mockito.verify(attachmentUtils).retrieveAttachments(any(), any(), eq(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS), eq(F24ResolutionMode.RESOLVE_WITH_REPLACED_LIST), any(), any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationAttachments_CfgSimpleRegisteredLetterAndAnalogNotification_CaseSimpleRegisteredLetter() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotificationWithPayments();

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().getFirst());

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(any(), (Integer) any(), any())).thenReturn(auditLogEvent);
        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());


        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS);

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult, ReceiverAddressSourceTypeInt.MITTENTE);

        // THEN
        Mockito.verify(attachmentUtils).retrieveAttachments(any(), any(), eq(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS), eq(F24ResolutionMode.RESOLVE_WITH_REPLACED_LIST), any(), any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationAttachments_CfgAnalogNotification() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification();

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(any(), (Integer) any(), any())).thenReturn(auditLogEvent);

        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS);

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult, ReceiverAddressSourceTypeInt.MITTENTE);

        // THEN
        Mockito.verify(attachmentUtils).retrieveAttachments(any(),any(),eq(SendAttachmentMode.AAR_DOCUMENTS), eq(F24ResolutionMode.RESOLVE_WITH_REPLACED_LIST),any(),any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationAttachments_CfgSimpleRegisteredLetterAndAnalogNotification_CaseAnalogNotification() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification();

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(any(), (Integer) any(), any())).thenReturn(auditLogEvent);

        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS);

        Mockito.when(attachmentUtils.retrieveAttachments(Mockito.any(),Mockito.any(),eq(SendAttachmentMode.AAR_DOCUMENTS), eq(F24ResolutionMode.RESOLVE_WITH_REPLACED_LIST),Mockito.any(),Mockito.any())).thenReturn(List.of("documentTest"));
        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult, ReceiverAddressSourceTypeInt.MITTENTE);

        // THEN
        Mockito.verify(attachmentUtils).retrieveAttachments(any(),any(),eq(SendAttachmentMode.AAR_DOCUMENTS), eq(F24ResolutionMode.RESOLVE_WITH_REPLACED_LIST),any(), any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationAttachments_CategorizedAttachments() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification();

        ResultFilterInt acceptedAttachments = ResultFilterInt.builder().fileKey("filekey").result(ResultFilterEnum.SUCCESS).reasonCode("reasonCode").reasonDescription("reasonDescription").build();

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(List.of(acceptedAttachments)).discardedAttachments(new ArrayList<>()).build();
        System.out.println(categorizedAttachmentsResult.getAcceptedAttachments());
        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(any(), (Integer) any(), any())).thenReturn(auditLogEvent);

        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult, ReceiverAddressSourceTypeInt.MITTENTE);

        // THEN
        Mockito.verify(attachmentUtils, Mockito.never()).retrieveAttachments(any(),any(),any(SendAttachmentMode.class), eq(F24ResolutionMode.RESOLVE_WITH_REPLACED_LIST),any(),any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotificationSchedulesTimeout() {
        NotificationInt notification = newNotification();
        String iun = notification.getIun();
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(any(), (Integer) any(), any())).thenReturn(auditLogEvent);

        String sendAnalogElementId = "sendAnalogElementId";
        Mockito.when(paperChannelUtils.addSendAnalogNotificationToTimeline(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(sendAnalogElementId);

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .iun(iun)
                .elementId(sendAnalogElementId)
                .timestamp(Instant.now())
                .build();
        Mockito.when(timelineService.getTimelineElement(iun, sendAnalogElementId)).thenReturn(Optional.of(timelineElementInternal));

        TimeParams timeParams = Mockito.mock(TimeParams.class);
        Mockito.when(timeParams.getScheduleAnalogWorkflowTimeoutOffset()).thenReturn(Duration.ofHours(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(timeParams);

        PhysicalAddressInt receiverAddress = PhysicalAddressInt.builder().address("address").fullname("fullname").build();
        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder()
                .acceptedAttachments(Collections.emptyList())
                .discardedAttachments(Collections.emptyList())
                .build();

        paperChannelService.sendAnalogNotification(notification, 0, 0, "req123", receiverAddress, "RIR", Collections.emptyList(), categorizedAttachmentsResult, null);

        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
        Mockito.verify(schedulerService).scheduleEvent(
                eq(iun),
                eq(0),
                any(Instant.class),
                eq(ActionType.ANALOG_WORKFLOW_NO_FEEDBACK_TIMEOUT),
                anyString(),
                any()
        );
        Assertions.assertEquals(iun, notification.getIun());
    }

    @ExtendWith(MockitoExtension.class)
    @ParameterizedTest
    @EnumSource(value = ReceiverAddressSourceTypeInt.class, names = {"MITTENTE", "ANPR", "REGISTRO_IMPRESE"})
    void prepareAnalogNotificationSecondAttemptWhenPaid_withSendAnalogTimeoutCreationRequest(
            ReceiverAddressSourceTypeInt source) {
        //GIVEN
        NotificationInt notificationInt = newNotificationWithPayments();
        PhysicalAddressInt previousAddress = PhysicalAddressInt.builder()
                .address("Via Roma 1").fullname("Destinatario").build();
        Mockito.when(paperChannelUtils.resolveAddressSource(notificationInt.getIun(), 0)).thenReturn(source);
        Mockito.when(paperChannelUtils.buildPrepareAnalogDomicileEventId(notificationInt, 0, 0))
                .thenReturn("previous-prepare");
        Mockito.when(paperChannelUtils.buildPrepareAnalogDomicileEventId(notificationInt, 0, 1))
                .thenReturn("current-prepare");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.lenient().when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(), anyInt())).thenReturn(notificationInt.getRecipients().getFirst());

        TimelineElementInternal previousSendEvent = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(previousSendEvent.getDetails()).thenReturn(
                SendAnalogDetailsInt.builder().physicalAddress(previousAddress).build());

        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS);
        Mockito.when(analogDeliveryTimeoutUtils.isSendAnalogTimeoutCreationRequestPresent(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(true);
        Mockito.when(paperChannelUtils.buildSendAnalogDomicileEventId(eq(notificationInt), Mockito.anyInt(), Mockito.anyInt())).thenReturn(null);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.any())).thenReturn(previousSendEvent);

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 1);

        // THEN
        ArgumentCaptor<PaperChannelPrepareRequest> captor = ArgumentCaptor.forClass(PaperChannelPrepareRequest.class);
        Mockito.verify(paperChannelSendClient).prepare(captor.capture());
        PaperChannelPrepareRequest request = captor.getValue();
        Assertions.assertAll(
                () -> Assertions.assertEquals(source, request.getReceiverAddressSourceType()),
                () -> Assertions.assertEquals("current-prepare", request.getRequestId()),
                () -> Assertions.assertEquals("previous-prepare", request.getRelatedRequestId()),
                () -> Assertions.assertSame(previousAddress, request.getPaAddress()),
                () -> Assertions.assertNull(request.getDiscoveredAddress())
        );
        Mockito.verify(paperChannelUtils).resolveAddressSource(notificationInt.getIun(), 0);
        Mockito.verify(attachmentUtils).retrieveAttachments(Mockito.any(),Mockito.any(),eq(SendAttachmentMode.AAR_DOCUMENTS), eq(F24ResolutionMode.URL),Mockito.any(),Mockito.any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
        Mockito.verify(paperChannelUtils, Mockito.never()).buildSendAnalogFeedbackEventId(eq(notificationInt), Mockito.anyInt(), Mockito.anyInt());
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationWithAarAndDocument_withoutSendAnalogTimeoutCreationRequest() {
        //GIVEN
        NotificationInt notificationInt = newNotificationWithPayments();
        PhysicalAddressInt previousAddress = PhysicalAddressInt.builder()
                .address("Via Roma 1").fullname("Destinatario").build();
        PhysicalAddressInt discoveredAddress = PhysicalAddressInt.builder()
                .address("Via Milano 2").fullname("Destinatario").build();
        Mockito.when(paperChannelUtils.resolveAddressSource(notificationInt.getIun(), 0))
                .thenReturn(ReceiverAddressSourceTypeInt.MITTENTE);
        Mockito.when(paperChannelUtils.buildPrepareAnalogDomicileEventId(notificationInt, 0, 0))
                .thenReturn("previous-prepare");
        Mockito.when(paperChannelUtils.buildPrepareAnalogDomicileEventId(notificationInt, 0, 1))
                .thenReturn("current-prepare");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(), anyInt())).thenReturn(notificationInt.getRecipients().getFirst());

        TimelineElementInternal previousSendEvent = TimelineElementInternal.builder()
                .details(SendAnalogDetailsInt.builder().physicalAddress(previousAddress).build()).build();
        TimelineElementInternal previousFeedbackEvent = TimelineElementInternal.builder()
                .details(SendAnalogFeedbackDetailsInt.builder().newAddress(discoveredAddress).build()).build();

        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS);
        Mockito.when(analogDeliveryTimeoutUtils.isSendAnalogTimeoutCreationRequestPresent(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(paperChannelUtils.buildSendAnalogDomicileEventId(notificationInt, 0, 0)).thenReturn("previous-send");
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(notificationInt.getIun(), "previous-send")).thenReturn(previousSendEvent);
        Mockito.when(paperChannelUtils.buildSendAnalogFeedbackEventId(notificationInt, 0, 0)).thenReturn("previous-feedback");
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(notificationInt.getIun(), "previous-feedback")).thenReturn(previousFeedbackEvent);

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 1);

        // THEN
        ArgumentCaptor<PaperChannelPrepareRequest> captor = ArgumentCaptor.forClass(PaperChannelPrepareRequest.class);
        Mockito.verify(paperChannelSendClient).prepare(captor.capture());
        PaperChannelPrepareRequest request = captor.getValue();
        Assertions.assertAll(
                () -> Assertions.assertEquals(ReceiverAddressSourceTypeInt.MITTENTE, request.getReceiverAddressSourceType()),
                () -> Assertions.assertEquals("current-prepare", request.getRequestId()),
                () -> Assertions.assertEquals("previous-prepare", request.getRelatedRequestId()),
                () -> Assertions.assertSame(previousAddress, request.getPaAddress()),
                () -> Assertions.assertSame(discoveredAddress, request.getDiscoveredAddress())
        );
        Mockito.verify(paperChannelUtils).resolveAddressSource(notificationInt.getIun(), 0);
        Mockito.verify(attachmentUtils).retrieveAttachments(Mockito.any(),Mockito.any(),eq(SendAttachmentMode.AAR_DOCUMENTS), eq(F24ResolutionMode.URL),Mockito.any(),Mockito.any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
        Mockito.verify(paperChannelUtils).buildSendAnalogFeedbackEventId(eq(notificationInt), Mockito.anyInt(), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @ParameterizedTest
    @CsvSource({
            "true, false, INDAGINE, true",
            "false, false, ANPR, false",
            "false, true, ANPR, false"
    })
    void sendAnalogNotificationSecondAttemptPropagatesSourceAndInvestigation(
            boolean discoveredAddressPresent, boolean timeoutPresent,
            ReceiverAddressSourceTypeInt source, boolean expectedInvestigation) {
        NotificationInt notification = newNotification();
        PhysicalAddressInt receiverAddress = PhysicalAddressInt.builder()
                .address("Via destinazione 1").fullname("Destinatario").build();
        PhysicalAddressInt discoveredAddress = discoveredAddressPresent
                ? PhysicalAddressInt.builder().address("Via indagine 2").build() : null;
        CategorizedAttachmentsResultInt attachments = CategorizedAttachmentsResultInt.builder()
                .acceptedAttachments(List.of(ResultFilterInt.builder().fileKey("attachment").build()))
                .discardedAttachments(Collections.emptyList())
                .build();
        SendResponse sendResponse = new SendResponse().amount(125);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(0),
                eq(PnAuditLogEventType.AUD_PD_EXECUTE), anyString(), any(), any(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(anyString(), any(Integer.class), any())).thenReturn(auditLogEvent);
        Mockito.when(notificationUtils.getRecipientFromIndex(notification, 0))
                .thenReturn(notification.getRecipients().getFirst());
        Mockito.when(analogDeliveryTimeoutUtils.isSendAnalogTimeoutCreationRequestPresent(
                notification.getIun(), 0, 0)).thenReturn(timeoutPresent);
        if (!timeoutPresent) {
            Mockito.when(paperChannelUtils.buildSendAnalogFeedbackEventId(notification, 0, 0))
                    .thenReturn("previous-feedback");
            Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(
                    notification.getIun(), "previous-feedback"))
                    .thenReturn(TimelineElementInternal.builder()
                            .details(SendAnalogFeedbackDetailsInt.builder().newAddress(discoveredAddress).build())
                            .build());
        }
        Mockito.when(paperChannelUtils.buildPrepareAnalogDomicileEventId(notification, 0, 0))
                .thenReturn("previous-prepare");
        Mockito.when(paperChannelSendClient.send(any())).thenReturn(sendResponse);
        Mockito.when(paperChannelUtils.addSendAnalogNotificationToTimeline(
                eq(notification), same(receiverAddress), eq(0), any(), eq(Collections.emptyList()),
                same(attachments), eq("current-prepare"))).thenReturn("current-send");

        String timelineId = paperChannelService.sendAnalogNotification(
                notification, 0, 1, "current-prepare", receiverAddress, "AR",
                Collections.emptyList(), attachments, source);

        ArgumentCaptor<AnalogDtoInt> dtoCaptor = ArgumentCaptor.forClass(AnalogDtoInt.class);
        Mockito.verify(paperChannelUtils).addSendAnalogNotificationToTimeline(
                eq(notification), same(receiverAddress), eq(0), dtoCaptor.capture(),
                eq(Collections.emptyList()), same(attachments), eq("current-prepare"));
        AnalogDtoInt dto = dtoCaptor.getValue();
        ArgumentCaptor<PaperChannelSendRequest> requestCaptor =
                ArgumentCaptor.forClass(PaperChannelSendRequest.class);
        Mockito.verify(paperChannelSendClient).send(requestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals("current-send", timelineId),
                () -> Assertions.assertEquals(source, dto.getReceiverAddressSourceType()),
                () -> Assertions.assertEquals(Boolean.valueOf(expectedInvestigation), dto.getHasOnSiteInvestigation()),
                () -> Assertions.assertEquals(1, dto.getSentAttemptMade()),
                () -> Assertions.assertEquals("previous-prepare", dto.getRelatedRequestId()),
                () -> Assertions.assertEquals("current-prepare", dto.getPrepareRequestId()),
                () -> Assertions.assertEquals("AR", dto.getProductType()),
                () -> Assertions.assertSame(sendResponse, dto.getSendResponse()),
                () -> Assertions.assertSame(receiverAddress, requestCaptor.getValue().getReceiverAddress()),
                () -> Assertions.assertEquals(List.of("attachment"), requestCaptor.getValue().getAttachments())
        );
        Mockito.verify(analogDeliveryTimeoutUtils).isSendAnalogTimeoutCreationRequestPresent(
                notification.getIun(), 0, 0);
        if (timeoutPresent) {
            Mockito.verify(paperChannelUtils, Mockito.never())
                    .buildSendAnalogFeedbackEventId(any(), anyInt(), anyInt());
            Mockito.verify(paperChannelUtils, Mockito.never())
                    .getPaperChannelNotificationTimelineElement(anyString(), anyString());
        } else {
            Mockito.verify(paperChannelUtils).getPaperChannelNotificationTimelineElement(
                    notification.getIun(), "previous-feedback");
        }
        Mockito.verifyNoInteractions(schedulerService);
        Mockito.verify(auditLogEvent).generateSuccess(
                "send success cost={} send timelineId={}", 125, "current-send");
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(anyString(), any(Object[].class));
    }

    private NotificationInt newNotification() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("taxid")
                                .internalId("taxid" + "ANON")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }

    private NotificationInt newNotificationWithPayments() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .documents(List.of(NotificationDocumentInt.builder()
                        .ref(NotificationDocumentInt.Ref.builder().key("DocumentKey").build())
                        .digests(NotificationDocumentInt.Digests.builder().sha256("DocumentSha256").build())
                        .build()))
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("taxid")
                                .internalId("taxid" + "ANON")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .payments(List.of(
                                        NotificationPaymentInfoInt.builder()
                                                .f24(F24Int.builder()
                                                        .applyCost(true)
                                                        .title("title")
                                                        .metadataAttachment(NotificationDocumentInt.builder()
                                                                .ref(NotificationDocumentInt.Ref.builder().key("F24Key").build())
                                                                .digests(NotificationDocumentInt.Digests.builder().sha256("F24Sha256").build())
                                                                .build())
                                                        .build())
                                                .pagoPA(PagoPaInt.builder()
                                                        .applyCost(true)
                                                        .attachment((NotificationDocumentInt.builder()
                                                                .ref(NotificationDocumentInt.Ref.builder().key("PagoPaKey").build())
                                                                .digests(NotificationDocumentInt.Digests.builder().sha256("PagoPaSha256").build())
                                                                .build()))
                                                        .creditorTaxId("taxid")
                                                        .noticeCode("noticeCode")
                                                        .build())
                                                .build()
                                ))
                                .build()
                ))
                .build();
    }

}