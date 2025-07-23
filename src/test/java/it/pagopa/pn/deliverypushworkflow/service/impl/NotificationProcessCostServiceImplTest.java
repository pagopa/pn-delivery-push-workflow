package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypushworkflow.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypushworkflow.dto.cost.UpdateNotificationCostResultInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.NotificationRequestAcceptedDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SimpleRegisteredLetterDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.NotificationFeePolicy;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResult;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.externalregistry.PnExternalRegistriesClientReactive;
import it.pagopa.pn.deliverypushworkflow.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

class NotificationProcessCostServiceImplTest {
    @Mock
    private TimelineService timelineService;
    @Mock
    private PnExternalRegistriesClientReactive pnExternalRegistriesClientReactive;
    @Mock
    private PnDeliveryPushWorkflowConfigs cfg;
    
    private NotificationProcessCostService service;

    Integer notificationCost = 100;
    Integer notificationFee = 99;
    Integer notificationVat = 22;
    @BeforeEach
    void setUp() {
        Mockito.when(cfg.getPagoPaNotificationBaseCost()).thenReturn(notificationCost);
        Mockito.when(cfg.getPagoPaNotificationFee()).thenReturn(notificationFee);
        Mockito.when(cfg.getPagoPaNotificationVat()).thenReturn(notificationVat);

        service = new NotificationProcessCostServiceImpl(timelineService, pnExternalRegistriesClientReactive, cfg);
    }
    
    @Test
    @ExtendWith(SpringExtension.class)
    void getPagoPaNotificationBaseCost() {
        Integer pagoPaBaseCost = service.getSendFeeAsync().block();

        Assertions.assertEquals(notificationCost, pagoPaBaseCost);
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
        Assertions.assertNotNull(updateNotificationCostResponseInt.getUpdateResults().get(0));
        
        UpdateNotificationCostResultInt updateNotificationCostResultInt = updateNotificationCostResponseInt.getUpdateResults().get(0);
        final UpdateNotificationCostResult updateNotificationCostResponseExpected = updateNotificationCostResponse.getUpdateResults().get(0);

        Assertions.assertEquals(updateNotificationCostResponseExpected.getResult().getValue(), updateNotificationCostResultInt.getResult().getValue());
        Assertions.assertEquals(updateNotificationCostResponseExpected.getNoticeCode(), updateNotificationCostResultInt.getPaymentsInfoForRecipient().getNoticeCode());
        Assertions.assertEquals(updateNotificationCostResponseExpected.getCreditorTaxId(), updateNotificationCostResultInt.getPaymentsInfoForRecipient().getCreditorTaxId());
        Assertions.assertEquals(updateNotificationCostResponseExpected.getRecIndex(), updateNotificationCostResultInt.getPaymentsInfoForRecipient().getRecIndex());
    }
    
    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCostF24_vat_paFee_version23() {
        //GIVEN

        // notifica singolo recipient
        // invio raccomandata semplice
        // nessun perfezionamento
        // DELIVERY_MODE

        String iun = "testIun";
        int recIndex = 0;

        TimelineElementInternal requestAccepted = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .details(new NotificationRequestAcceptedDetailsInt())
                .build();

        final int simpleRegisteredLetterCost = 1400;

        TimelineElementInternal sendSimpleRegisteredLetter = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER)
                .details(SimpleRegisteredLetterDetailsInt.builder()
                        .analogCost(simpleRegisteredLetterCost)
                        .recIndex(recIndex)
                        .build())
                .build();

        Set<TimelineElementInternal> timelineElements = new HashSet<>(Arrays.asList(requestAccepted, sendSimpleRegisteredLetter));

        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);

        int paFee = 0;
        int vat = 22;
        String version = "2.3";

        //WHEN
        Integer notificationCost = service.notificationProcessCostF24(
                iun,
                recIndex,
                NotificationFeePolicy.DELIVERY_MODE,
                paFee,
                vat,
                version
        ).block();

        //THEN
        int notificationProcessTotalCostExpected = getNotificationProcessTotalCostExpected(
                service.getSendFee(),
                simpleRegisteredLetterCost,
                paFee,
                vat
        );

        Assertions.assertNotNull(notificationCost);
        Assertions.assertEquals(notificationCost, notificationProcessTotalCostExpected);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCostF24_notVat_paFee_version23() {
        //GIVEN

        // notifica singolo recipient
        // invio raccomandata semplice
        // nessun perfezionamento
        // DELIVERY_MODE

        String iun = "testIun";
        int recIndex = 0;

        TimelineElementInternal requestAccepted = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .details(new NotificationRequestAcceptedDetailsInt())
                .build();

        final int simpleRegisteredLetterCost = 1400;

        TimelineElementInternal sendSimpleRegisteredLetter = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER)
                .details(SimpleRegisteredLetterDetailsInt.builder()
                        .analogCost(simpleRegisteredLetterCost)
                        .recIndex(recIndex)
                        .build())
                .build();

        Set<TimelineElementInternal> timelineElements = new HashSet<>(Arrays.asList(requestAccepted, sendSimpleRegisteredLetter));

        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);

        int paFee = 0;
        Integer vat = null;
        String version = "2.3";

        //WHEN
        final Mono<Integer> notificationProcessCostMono = service.notificationProcessCostF24(
                iun,
                recIndex,
                NotificationFeePolicy.DELIVERY_MODE,
                paFee,
                vat,
                version
        );

        Integer notificationCost  = notificationProcessCostMono.block();

        int notificationProcessPartialCostExpected = getNotificationProcessTotalCostExpected(
                service.getSendFee(),
                simpleRegisteredLetterCost,
                paFee,
                notificationVat
        );

        Assertions.assertNotNull(notificationCost);
        Assertions.assertEquals(notificationCost, notificationProcessPartialCostExpected);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCostF24_notVat_paFee_version21() {
        //GIVEN

        // notifica singolo recipient
        // invio raccomandata semplice
        // nessun perfezionamento
        // DELIVERY_MODE

        String iun = "testIun";
        int recIndex = 0;

        TimelineElementInternal requestAccepted = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .details(new NotificationRequestAcceptedDetailsInt())
                .build();

        final int simpleRegisteredLetterCost = 1400;

        TimelineElementInternal sendSimpleRegisteredLetter = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER)
                .details(SimpleRegisteredLetterDetailsInt.builder()
                        .analogCost(simpleRegisteredLetterCost)
                        .recIndex(recIndex)
                        .build())
                .build();

        Set<TimelineElementInternal> timelineElements = new HashSet<>(Arrays.asList(requestAccepted, sendSimpleRegisteredLetter));

        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);

        int paFee = 0;
        Integer vat = null;
        String version = "2.1";

        //WHEN
        Integer notificationCost = service.notificationProcessCostF24(
                        iun,
                        recIndex,
                        NotificationFeePolicy.DELIVERY_MODE,
                        paFee,
                        vat,
                        version
                ).block();


        //THEN
        int notificationProcessPartialCostExpected = getNotificationProcessTotalCostExpected(
                service.getSendFee(),
                simpleRegisteredLetterCost,
                paFee,
                notificationVat
        );

        Assertions.assertNotNull(notificationCost);
        Assertions.assertEquals(notificationCost, notificationProcessPartialCostExpected);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCostF24_notVat_paFee_notVersion() {
        //GIVEN

        // notifica singolo recipient
        // invio raccomandata semplice
        // nessun perfezionamento
        // DELIVERY_MODE

        String iun = "testIun";
        int recIndex = 0;

        TimelineElementInternal requestAccepted = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .details(new NotificationRequestAcceptedDetailsInt())
                .build();

        final int simpleRegisteredLetterCost = 1400;

        TimelineElementInternal sendSimpleRegisteredLetter = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER)
                .details(SimpleRegisteredLetterDetailsInt.builder()
                        .analogCost(simpleRegisteredLetterCost)
                        .recIndex(recIndex)
                        .build())
                .build();

        Set<TimelineElementInternal> timelineElements = new HashSet<>(Arrays.asList(requestAccepted, sendSimpleRegisteredLetter));

        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);

        int paFee = 0;
        Integer vat = null;
        String version = null;

        //WHEN
        Integer notificationCost = service.notificationProcessCostF24(
                iun,
                recIndex,
                NotificationFeePolicy.DELIVERY_MODE,
                paFee,
                vat,
                version
        ).block();


        //THEN
        int notificationProcessPartialCostExpected = getNotificationProcessTotalCostExpected(
                service.getSendFee(),
                simpleRegisteredLetterCost,
                paFee,
                notificationVat
        );

        Assertions.assertNotNull(notificationCost);
        Assertions.assertEquals(notificationCost, notificationProcessPartialCostExpected);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCostF24_vat_notPaFee_version23() {
        //GIVEN

        // notifica singolo recipient
        // invio raccomandata semplice
        // nessun perfezionamento
        // DELIVERY_MODE

        String iun = "testIun";
        int recIndex = 0;

        TimelineElementInternal requestAccepted = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .details(new NotificationRequestAcceptedDetailsInt())
                .build();

        final int simpleRegisteredLetterCost = 1400;

        TimelineElementInternal sendSimpleRegisteredLetter = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER)
                .details(SimpleRegisteredLetterDetailsInt.builder()
                        .analogCost(simpleRegisteredLetterCost)
                        .recIndex(recIndex)
                        .build())
                .build();

        Set<TimelineElementInternal> timelineElements = new HashSet<>(Arrays.asList(requestAccepted, sendSimpleRegisteredLetter));

        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);

        Integer paFee = null;
        Integer vat = 22;
        String version = "2.3";

        //WHEN
        final Mono<Integer> notificationProcessCostMono = service.notificationProcessCostF24(
                iun,
                recIndex,
                NotificationFeePolicy.DELIVERY_MODE,
                paFee,
                vat,
                version
        );

        Integer notificationCost  = notificationProcessCostMono.block();

        int notificationProcessPartialCostExpected = getNotificationProcessTotalCostExpected(
                service.getSendFee(),
                simpleRegisteredLetterCost,
                notificationFee,
                notificationVat
        );

        Assertions.assertNotNull(notificationCost);
        Assertions.assertEquals(notificationCost, notificationProcessPartialCostExpected);
    }

    private static Integer getNotificationProcessTotalCostExpected(int pagoPaBaseCost, int analogCost, Integer paFee, Integer vat) {
        if (paFee != null && vat != null){
            int analogCostWithVat = getAnalogCostWithVat(vat, analogCost);
            return pagoPaBaseCost + analogCostWithVat + paFee;
        }
        return null;
    }

    private static Integer getAnalogCostWithVat(Integer vat, Integer analogCost) {
        return vat != null ? analogCost + (analogCost * vat / 100) : null;
    }
}