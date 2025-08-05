package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypushworkflow.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypushworkflow.dto.cost.UpdateNotificationCostResultInt;
import it.pagopa.pn.deliverypushworkflow.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.NotificationCancelledDocumentCreationRequestDetailsInt;
import it.pagopa.pn.deliverypushworkflow.exceptions.PnPaymentUpdateRetryException;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.NotificationFeePolicy;
import it.pagopa.pn.deliverypushworkflow.service.*;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.deliverypushworkflow.action.it.utils.TestUtils.verifyPaymentInfo;
import static org.mockito.ArgumentMatchers.*;

class NotificationCancellationServiceImplTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private PaperNotificationFailedService paperNotificationFailedService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationProcessCostService notificationProcessCostService;
    @Mock
    private SaveLegalFactsService saveLegalFactsService;
    @Mock
    private DocumentCreationRequestService documentCreationRequestService;

    private NotificationCancellationService notificationCancellationService;
    
    @BeforeEach
    public void init(){
        notificationCancellationService = new NotificationCancellationServiceImpl(notificationService, paperNotificationFailedService, timelineService, timelineUtils, auditLogService, notificationProcessCostService, saveLegalFactsService, documentCreationRequestService);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void cancelNotificationForPagoPaAsync() {
        //Given
        final String creditorTaxId = "cred";
        final String noticeCode = "notice";
        final String legalFactId = "legalFactId";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(PagoPaInt.builder()
                                        .creditorTaxId(creditorTaxId)
                                        .noticeCode(noticeCode)
                                        .applyCost(true)
                                        .build()
                                )
                                .build()
                ))
                .build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.ASYNC)
                .withPaFee(100)
                .build();

        final TimelineElementInternal timelineElement = TimelineElementInternal.builder()
                .details(NotificationCancelledDocumentCreationRequestDetailsInt.builder()
                        .legalFactId(legalFactId)
                        .build())
                .timestamp(Instant.now())
                .build();
        Mockito.when(timelineUtils.buildNotificationCancelledLegalFactCreationRequest(notification, legalFactId)).thenReturn(timelineElement);
        Mockito.when(timelineService.addTimelineElement(Mockito.any(), Mockito.any())).thenReturn(false);
        Mockito.when(timelineService.getTimelineElement(any(), any())).thenReturn(Optional.of(timelineElement));
        Mockito.when(notificationService.removeAllNotificationCostsByIun(notification.getIun())).thenReturn(Mono.empty());
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(2)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);
        Mockito.when(saveLegalFactsService.sendCreationRequestForNotificationCancelledLegalFact(notification, timelineElement.getTimestamp())).thenReturn(legalFactId);
        Mockito.doNothing().when(documentCreationRequestService).addDocumentCreationRequest(legalFactId, notification.getIun(), DocumentCreationTypeInt.NOTIFICATION_CANCELLED, timelineElement.getElementId());

        final int recIndex = 0;
        UpdateNotificationCostResponseInt response = UpdateNotificationCostResponseInt.builder()
                .updateResults(Collections.singletonList(UpdateNotificationCostResultInt.builder()
                        .result(UpdateNotificationCostResultInt.ResultEnum.KO)
                        .paymentsInfoForRecipient(PaymentsInfoForRecipientInt.builder()
                                .creditorTaxId(creditorTaxId)
                                .noticeCode(noticeCode)
                                .recIndex(recIndex)
                                .build())
                        .build()
                )).build();

        Mockito.when(notificationProcessCostService.setNotificationStepCost(
                Mockito.anyInt(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class),
                Mockito.any(UpdateCostPhaseInt.class)
        )).thenReturn(Mono.just(response));

        //WHEN
        notificationCancellationService.continueCancellationProcess(notification.getIun());

        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElement, notification);
        Mockito.verify(notificationService).removeAllNotificationCostsByIun(notification.getIun());
        Mockito.verify(auditLogEvent).generateSuccess();

        ArgumentCaptor<List<PaymentsInfoForRecipientInt>> paymentForRecipientListCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(notificationProcessCostService).setNotificationStepCost(
                Mockito.eq(NotificationCancellationServiceImpl.NOTIFICATION_CANCELLED_COST),
                Mockito.eq(notification.getIun()),
                paymentForRecipientListCaptor.capture(),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class),
                Mockito.eq(UpdateCostPhaseInt.NOTIFICATION_CANCELLED)
        );

        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipientsCaptured = paymentForRecipientListCaptor.getValue();
        verifyPaymentInfo(notification, recIndex, paymentsInfoForRecipientsCaptured);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void cancelNotificationForPagoPaAsyncError() {
        //Given
        final String creditorTaxId = "cred";
        final String noticeCode = "notice";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(PagoPaInt.builder()
                                        .creditorTaxId(creditorTaxId)
                                        .noticeCode(noticeCode)
                                        .applyCost(true)
                                        .build()
                                )
                                .build()
                ))
                .build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.ASYNC)
                .withPaFee(100)
                .build();

        final String iun = notification.getIun();
        Mockito.when(notificationService.removeAllNotificationCostsByIun(iun)).thenReturn(Mono.empty());
        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(iun), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(2)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateFailure(Mockito.anyString(), (String) Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);

        final int recIndex = 0;
        UpdateNotificationCostResponseInt response = UpdateNotificationCostResponseInt.builder()
                .updateResults(Collections.singletonList(UpdateNotificationCostResultInt.builder()
                        .result(UpdateNotificationCostResultInt.ResultEnum.RETRY)
                        .paymentsInfoForRecipient(PaymentsInfoForRecipientInt.builder()
                                .creditorTaxId(creditorTaxId)
                                .noticeCode(noticeCode)
                                .recIndex(recIndex)
                                .build())
                        .build()
                )).build();

        Mockito.when(notificationProcessCostService.setNotificationStepCost(
                Mockito.anyInt(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class),
                Mockito.any(UpdateCostPhaseInt.class)
        )).thenReturn(Mono.just(response));

        //WHEN
        Assertions.assertThrows(PnPaymentUpdateRetryException.class,
                () -> notificationCancellationService.continueCancellationProcess(iun));

        //THEN
        Mockito.verify(notificationService).removeAllNotificationCostsByIun(iun);
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(Mockito.any(), Mockito.eq(notification));
        Mockito.verify(auditLogEvent).generateFailure(Mockito.anyString(), (String) Mockito.any(), Mockito.any());

        ArgumentCaptor<List<PaymentsInfoForRecipientInt>> paymentForRecipientListCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(notificationProcessCostService).setNotificationStepCost(
                Mockito.eq(NotificationCancellationServiceImpl.NOTIFICATION_CANCELLED_COST),
                Mockito.eq(iun),
                paymentForRecipientListCaptor.capture(),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class),
                Mockito.eq(UpdateCostPhaseInt.NOTIFICATION_CANCELLED)
        );

        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipientsCaptured = paymentForRecipientListCaptor.getValue();
        verifyPaymentInfo(notification, recIndex, paymentsInfoForRecipientsCaptured);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void cancelNotificationNotPagoPaAsync() {
        //Given
        final String creditorTaxId = "cred";
        final String noticeCode = "notice";
        final String legalFactId = "legalFactId";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(PagoPaInt.builder()
                                        .creditorTaxId(creditorTaxId)
                                        .noticeCode(noticeCode)
                                        .applyCost(true)
                                        .build()
                                )
                                .build()
                ))
                .build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.NONE)
                .withPaFee(100)
                .build();

        final TimelineElementInternal timelineElement = TimelineElementInternal.builder()
                .details(NotificationCancelledDocumentCreationRequestDetailsInt.builder()
                        .legalFactId(legalFactId)
                        .build())
                .timestamp(Instant.now())
                .build();
        Mockito.when(timelineUtils.buildNotificationCancelledLegalFactCreationRequest(notification, legalFactId)).thenReturn(timelineElement);
        Mockito.when(timelineService.addTimelineElement(Mockito.any(), Mockito.any())).thenReturn(false);
        Mockito.when(timelineService.getTimelineElement(any(), any())).thenReturn(Optional.of(timelineElement));
        Mockito.when(notificationService.removeAllNotificationCostsByIun(notification.getIun())).thenReturn(Mono.empty());
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(2)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);
        Mockito.when(saveLegalFactsService.sendCreationRequestForNotificationCancelledLegalFact(notification, timelineElement.getTimestamp())).thenReturn(legalFactId);
        Mockito.doNothing().when(documentCreationRequestService).addDocumentCreationRequest(legalFactId, notification.getIun(), DocumentCreationTypeInt.NOTIFICATION_CANCELLED, timelineElement.getElementId());

        //WHEN
        notificationCancellationService.continueCancellationProcess(notification.getIun());

        //THEN
        Mockito.verify(paperNotificationFailedService).deleteNotificationFailed(recipient.getInternalId(), "iun");
        Mockito.verify(timelineService).addTimelineElement(timelineElement, notification);
        Mockito.verify(notificationService).removeAllNotificationCostsByIun(notification.getIun());
        Mockito.verify(auditLogEvent).generateSuccess();

        Mockito.verify(notificationProcessCostService, Mockito.never()).setNotificationStepCost(
                Mockito.eq(NotificationCancellationServiceImpl.NOTIFICATION_CANCELLED_COST),
                Mockito.eq(notification.getIun()),
                Mockito.any(),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class),
                Mockito.eq(UpdateCostPhaseInt.NOTIFICATION_CANCELLED)
        );
    }


    @Test
    @ExtendWith(SpringExtension.class)
    void cancelNotificationAlreadyInserted() {
        //Given
        final String legalFactId = "legalFactId";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();

        final TimelineElementInternal timelineElementOLD = TimelineElementInternal.builder()
                .details(NotificationCancelledDocumentCreationRequestDetailsInt.builder()
                        .legalFactId(legalFactId)
                        .build())
                .timestamp(Instant.now().minusMillis(1000))
                .build();
        final TimelineElementInternal timelineElement = TimelineElementInternal.builder()
                .details(NotificationCancelledDocumentCreationRequestDetailsInt.builder()
                        .legalFactId(legalFactId)
                        .build())
                .timestamp(Instant.now())
                .build();
        Mockito.when(timelineUtils.buildNotificationCancelledLegalFactCreationRequest(any(), any())).thenReturn(timelineElement);
        Mockito.when(timelineService.addTimelineElement(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(timelineService.getTimelineElement(any(), any())).thenReturn(Optional.of(timelineElementOLD));
        Mockito.when(notificationService.removeAllNotificationCostsByIun(notification.getIun())).thenReturn(Mono.empty());
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(2)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);
        Mockito.when(saveLegalFactsService.sendCreationRequestForNotificationCancelledLegalFact(notification, timelineElement.getTimestamp())).thenReturn(legalFactId);
        Mockito.doNothing().when(documentCreationRequestService).addDocumentCreationRequest(legalFactId, notification.getIun(), DocumentCreationTypeInt.NOTIFICATION_CANCELLED, timelineElement.getElementId());

        //WHEN
        notificationCancellationService.continueCancellationProcess(notification.getIun());

        //THEN
        Mockito.verify(paperNotificationFailedService).deleteNotificationFailed(recipient.getInternalId(), "iun");
        Mockito.verify(timelineService).addTimelineElement(timelineElement, notification);
        Mockito.verify(notificationService).removeAllNotificationCostsByIun(notification.getIun());
        Mockito.verify(auditLogEvent).generateSuccess();
    }


    @Test
    @ExtendWith(SpringExtension.class)
    void cancelNotificationException() {
        //Given
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();

        Mockito.when(notificationService.removeAllNotificationCostsByIun(notification.getIun())).thenThrow(new NullPointerException());
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(2)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateFailure(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN
        String iun = notification.getIun();
        Assert.assertThrows(NullPointerException.class, ()-> notificationCancellationService.continueCancellationProcess(iun));

        //THEN
        Mockito.verify(paperNotificationFailedService, Mockito.never()).deleteNotificationFailed(recipient.getInternalId(), "iun");
        Mockito.verify(auditLogEvent).generateFailure(Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void completeCancellationProcessSuccess() {
        // Given
        String iun = "testIun";
        String legalFactId = "testLegalFactId";
        NotificationInt notification = getNotification();
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        final TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();

        Mockito.when(auditLogService.buildAuditLogEvent(eq(iun), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(3)))
                .thenReturn(auditLogEvent);
        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        Mockito.when(timelineUtils.buildCancelledTimelineElement(any(), any())).thenReturn(timelineElement);
        Mockito.when(timelineService.addTimelineElement(Mockito.any(), Mockito.any())).thenReturn(false);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        // When
        notificationCancellationService.completeCancellationProcess(iun, legalFactId);

        // Then
        Mockito.verify(timelineService).addTimelineElement(timelineElement, notification);
        Mockito.verify(auditLogService).buildAuditLogEvent(eq(iun), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(3));
        Mockito.verify(auditLogEvent).generateSuccess();
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void completeCancellationProcessFailure() {
        // Given
        String iun = "testIun";
        String legalFactId = "testLegalFactId";
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);

        Mockito.when(auditLogService.buildAuditLogEvent(eq(iun), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(3)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateFailure(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(notificationService.getNotificationByIun(iun)).thenThrow(new PnInternalException("Test Exception", "TestCode"));

        // When & Then
        Assertions.assertThrows(PnInternalException.class, () -> notificationCancellationService.completeCancellationProcess(iun, legalFactId));
        Mockito.verify(auditLogEvent).generateFailure(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any());
    }

    private NotificationInt getNotification(){
        return NotificationInt.builder()
                .iun("RUYX-MLZA-JUPJ-202308-W-1")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId("Milano1")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .physicalAddress(
                                        PhysicalAddressInt.builder()
                                                .address("test address")
                                                .build()
                                )
                                .payments(null)
                                .build()
                ))
                .build();

    }
}