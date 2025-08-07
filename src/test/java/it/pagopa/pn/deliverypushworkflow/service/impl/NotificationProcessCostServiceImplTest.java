package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypushworkflow.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypushworkflow.dto.cost.UpdateNotificationCostResultInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.NotificationFeePolicy;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.deliverypush.model.NotificationProcessCostResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResult;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.deliverypush.PnDeliveryPushClientReactive;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.externalregistry.PnExternalRegistriesClientReactive;
import it.pagopa.pn.deliverypushworkflow.service.NotificationProcessCostService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

class NotificationProcessCostServiceImplTest {
    private PnExternalRegistriesClientReactive pnExternalRegistriesClientReactive;
    private PnDeliveryPushClientReactive pnDeliveryPushClientReactive;
    private NotificationProcessCostService service;

    Integer notificationCostNumber = 100;
    @BeforeEach
    void setUp() {
        this.pnExternalRegistriesClientReactive = Mockito.mock(PnExternalRegistriesClientReactive.class);
        this.pnDeliveryPushClientReactive = Mockito.mock(PnDeliveryPushClientReactive.class);
        PnDeliveryPushWorkflowConfigs cfg = Mockito.mock(PnDeliveryPushWorkflowConfigs.class);

        Mockito.when(cfg.getPagoPaNotificationBaseCost()).thenReturn(notificationCostNumber);

        service = new NotificationProcessCostServiceImpl(pnExternalRegistriesClientReactive, cfg, pnDeliveryPushClientReactive);
    }
    
    @Test
    @ExtendWith(SpringExtension.class)
    void getPagoPaNotificationBaseCost() {
        Integer pagoPaBaseCost = service.getSendFeeAsync().block();

        Assertions.assertEquals(notificationCostNumber, pagoPaBaseCost);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void setNotificationStepCostOK() {
        //GIVEN
        int notificationStepCost = 100;
        String iun = "testIun";

        PaymentsInfoForRecipientInt paymentsInfoForRecipient = PaymentsInfoForRecipientInt.builder()
                .creditorTaxId("testCred")
                .noticeCode("testNotice")
                .recIndex(0)
                .build();
        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipients = Collections.singletonList(paymentsInfoForRecipient);
        Instant eventTimestamp = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant eventStorageTimestamp = Instant.now().minus(1, ChronoUnit.HOURS);
        UpdateCostPhaseInt updateCostPhase = UpdateCostPhaseInt.VALIDATION;
        
        UpdateNotificationCostResponse updateNotificationCostResponse = new UpdateNotificationCostResponse();
        updateNotificationCostResponse.addUpdateResultsItem(
                new UpdateNotificationCostResult()
                        .creditorTaxId(paymentsInfoForRecipient.getCreditorTaxId())
                        .noticeCode(paymentsInfoForRecipient.getNoticeCode())
                        .recIndex(paymentsInfoForRecipient.getRecIndex())
                        .result(UpdateNotificationCostResult.ResultEnum.KO)
        );
        Mockito.when(pnExternalRegistriesClientReactive.updateNotificationCost(Mockito.any(UpdateNotificationCostRequest.class))).thenReturn(Mono.just(updateNotificationCostResponse));
        
        //WHEN
        UpdateNotificationCostResponseInt updateNotificationCostResponseInt = service.setNotificationStepCost(notificationStepCost,iun,paymentsInfoForRecipients,eventTimestamp,
                eventStorageTimestamp, updateCostPhase).block();
        
        //THEN
        Assertions.assertNotNull(updateNotificationCostResponseInt);
        Assertions.assertNotNull(updateNotificationCostResponseInt.getUpdateResults());
        Assertions.assertNotNull(updateNotificationCostResponseInt.getUpdateResults().getFirst());
        
        UpdateNotificationCostResultInt updateNotificationCostResultInt = updateNotificationCostResponseInt.getUpdateResults().getFirst();
        final UpdateNotificationCostResult updateNotificationCostResponseExpected = updateNotificationCostResponse.getUpdateResults().getFirst();

        Assertions.assertEquals(updateNotificationCostResponseExpected.getResult().getValue(), updateNotificationCostResultInt.getResult().getValue());
        Assertions.assertEquals(updateNotificationCostResponseExpected.getNoticeCode(), updateNotificationCostResultInt.getPaymentsInfoForRecipient().getNoticeCode());
        Assertions.assertEquals(updateNotificationCostResponseExpected.getCreditorTaxId(), updateNotificationCostResultInt.getPaymentsInfoForRecipient().getCreditorTaxId());
        Assertions.assertEquals(updateNotificationCostResponseExpected.getRecIndex(), updateNotificationCostResultInt.getPaymentsInfoForRecipient().getRecIndex());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCostF24_vat_paFee_version23_withTotalCost() {
        //GIVEN
        String iun = "testIun";
        int recIndex = 0;
        int paFee = 0;
        int vat = 22;
        String version = "2.3";
        int notificationProcessTotalCostExpected = 100;
        NotificationProcessCostResponse notificationProcessCostResponse = new NotificationProcessCostResponse();
        notificationProcessCostResponse.setTotalCost(notificationProcessTotalCostExpected);

        Mockito.when(pnDeliveryPushClientReactive.getNotificationProcessCost(any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(notificationProcessCostResponse));

        //WHEN
        Integer notificationCostNumber = service.notificationProcessCostF24(
                iun,
                recIndex,
                NotificationFeePolicy.DELIVERY_MODE,
                paFee,
                vat,
                version
        ).block();

        //THEN
        Assertions.assertNotNull(notificationCostNumber);
        Assertions.assertEquals(notificationProcessTotalCostExpected, notificationCostNumber);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCostF24_vat_paFee_version22_withPartialCost() {
        //GIVEN
        String iun = "testIun";
        int recIndex = 0;
        int paFee = 0;
        int vat = 22;
        String version = "2.2";
        int notificationProcessTotalCostExpected = 50;
        NotificationProcessCostResponse notificationProcessCostResponse = new NotificationProcessCostResponse();
        notificationProcessCostResponse.setPartialCost(notificationProcessTotalCostExpected);

        Mockito.when(pnDeliveryPushClientReactive.getNotificationProcessCost(iun, recIndex, NotificationFeePolicy.DELIVERY_MODE, true, paFee, vat))
                .thenReturn(Mono.just(notificationProcessCostResponse));

        //WHEN
        Integer notificationCostNumber = service.notificationProcessCostF24(
                iun,
                recIndex,
                NotificationFeePolicy.DELIVERY_MODE,
                paFee,
                vat,
                version
        ).block();

        //THEN
        Assertions.assertNotNull(notificationCostNumber);
        Assertions.assertEquals(notificationProcessTotalCostExpected, notificationCostNumber);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCostF24_vat_paFee_version23_withPartialCost() {
        //GIVEN
        String iun = "testIun";
        int recIndex = 0;
        int paFee = 0;
        int vat = 22;
        String version = "2.3";
        int notificationProcessTotalCostExpected = 50;
        NotificationProcessCostResponse notificationProcessCostResponse = new NotificationProcessCostResponse();
        notificationProcessCostResponse.setPartialCost(notificationProcessTotalCostExpected);

        Mockito.when(pnDeliveryPushClientReactive.getNotificationProcessCost(any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(notificationProcessCostResponse));

        Mono<Integer> response = service.notificationProcessCostF24(
                iun,
                recIndex,
                NotificationFeePolicy.DELIVERY_MODE,
                paFee,
                vat,
                version
        );
        //WHEN
        StepVerifier.create(response)
                .expectError(PnInternalException.class)
                .verify();
    }
    
}