package it.pagopa.pn.deliverypushworkflow.action.it;

import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import it.pagopa.pn.deliverypushworkflow.action.choosedeliverymode.ChooseDeliveryModeUtilsImpl;
import it.pagopa.pn.deliverypushworkflow.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypushworkflow.action.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypushworkflow.action.it.mockbean.PaperChannelMock;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypushworkflow.action.notificationpaid.NotificationPaidHandler;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.ScheduleRecipientWorkflow;
import it.pagopa.pn.deliverypushworkflow.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypushworkflow.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypushworkflow.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.EventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.NotificationFeePolicy;
import it.pagopa.pn.deliverypushworkflow.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypushworkflow.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;

class NotificationPaidTestIT extends CommonTestConfiguration{
    @MockitoSpyBean
    LegalFactGenerator legalFactGenerator;
    @MockitoSpyBean
    ExternalChannelMock externalChannelMock;
    @MockitoSpyBean
    PaperChannelMock paperChannelMock;
    @MockitoSpyBean
    CompletionWorkFlowHandler completionWorkflow;
    @Autowired
    ScheduleRecipientWorkflow scheduleRecipientWorkflow;
    @Autowired
    TimelineService timelineService;
    @Autowired
    NotificationPaidHandler notificationPaidHandler;

    @Test
    void notificationPaidNoAnalogSend() {
 /*
       - Platform address vuoto (Ottenuto non valorizzando il platformAddress in addressBookEntry)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)

       - Indirizzo courtesy message presente, dunque inviato (Ottenuto valorizzando il courtesyAddress del addressBookEntry)
       - Viene pagata la notifica, questo comporta che nessun invio analogico avvenga
     */

        PhysicalAddressInt paPhysicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(ExternalChannelMock.EXT_CHANNEL_SEND_NEW_ADDR + ExternalChannelMock.EXTCHANNEL_SEND_FAIL + " Via Nuova")
                .build();

        String iun = TestUtils.getRandomIun();

        //Simulazione visualizzazione notifica a valle del send del messaggio di cortesi
        String taxId = "testTaxId";

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(paPhysicalAddress)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();

        
        pnDeliveryClientMock.addNotification(notification);

        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        

        //Start del workflow
        scheduleRecipientWorkflow.startScheduleRecipientWorkflow(iun);

        PnDeliveryPaymentEvent.Payload paymentEventPayload = simulateNotificationPaid(iun, recIndex, null);

        String timelineId = TimelineEventId.SCHEDULE_ANALOG_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build()
        );
        
        //Dal momento che l'ultimo elemento di timeline non viene inserito in prossimità della fine del workflow viene utilizzato un delay
        with().pollDelay(5, SECONDS).await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineId).isPresent())
        );

        //Viene verificato che gli indirizzi PLATFORM SPECIAL E GENERAL non siano presenti
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.SPECIAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);
        TestUtils.checkGetAddress(iun, recIndex, false, DigitalAddressSourceInt.GENERAL, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificata l'assenza degli invii verso external channel
        TestUtils.checkNotSendPaperToExtChannel(iun, recIndex, 0, timelineService);
        Mockito.verify(paperChannelMock, Mockito.times(0)).send(Mockito.any(PaperChannelSendRequest.class));

        //Viene verificato che la notifica sia stata pagata
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.NOTIFICATION_PAID.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .noticeCode(paymentEventPayload.getNoticeCode())
                                .creditorTaxId(paymentEventPayload.getCreditorTaxId())
                                .build())).isPresent());

        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .build();

        TestUtils.GeneratedLegalFactsPayload generatedLegalFactsPayload = TestUtils.GeneratedLegalFactsPayload.builder()
                .notification(notification)
                .recipient(recipient)
                .recIndex(recIndex)
                .generatedLegalFactsInfo(generatedLegalFactsInfo)
                .endWorkflowStatus(EndWorkflowStatus.SUCCESS)
                .legalFactGenerator(legalFactGenerator)
                .timelineService(timelineService)
                .sentPecAttemptNumber(0)
                .delegateInfo(null)
                .build();
        TestUtils.checkGeneratedLegalFacts(generatedLegalFactsPayload);
        
        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs("Notification is PAID but not VIEWED, should check how!");
    }

    @Test
    void digitalWorkflowNotificationPaidStillRefinement() {
     /*
       - Platform address presente e invio con successo (Ottenuto valorizzando il platformAddress in addressBookEntry con ExternalChannelMock.EXT_CHANNEL_WORKS)
       - Special address vuoto (Ottenuto non valorizzando il digitalDomicile del recipient)
       - General address vuoto (Ottenuto non valorizzando nessun digital address per il recipient in PUB_REGISTRY_DIGITAL)
    */
        LegalDigitalAddressInt platformAddress = LegalDigitalAddressInt.builder()
                .address("test@" + ExternalChannelMock.EXT_CHANNEL_WORKS)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        String taxid01 = "TAXID01";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxid01)
                .withInternalId("ANON_"+taxid01)
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withPaId("paId01")
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();


        pnDeliveryClientMock.addNotification(notification);
        addressBookMock.addLegalDigitalAddresses(recipient.getInternalId(), notification.getSender().getPaId(), Collections.singletonList(platformAddress));

        String iun = notification.getIun();
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //Start del workflow
        scheduleRecipientWorkflow.startScheduleRecipientWorkflow(iun);

        String timelineIdToWait = TimelineEventId.SCHEDULE_REFINEMENT_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build()
        );
        simulateNotificationPaid(iun, recIndex, timelineIdToWait);
        
        with().pollDelay(5, SECONDS).await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineIdToWait).isPresent())
        );
        
        //Viene verificata la presenza dell'indirizzo di piattaforma
        TestUtils.checkGetAddress(iun, recIndex, true, DigitalAddressSourceInt.PLATFORM, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, timelineService);

        //Viene verificato che sia stata effettuata una sola chiamata ad external channel
        int sentPecAttemptNumber = 1;
        Mockito.verify(externalChannelMock, Mockito.times(sentPecAttemptNumber)).sendLegalNotification(Mockito.any(NotificationInt.class), Mockito.any(), Mockito.any(LegalDigitalAddressInt.class), Mockito.anyString(),Mockito.anyList(), Mockito.anyString());
        
        //Viene verificato che il workflow abbia avuto successo
        TestUtils.checkSuccessDigitalWorkflow(iun, recIndex, timelineService, completionWorkflow, platformAddress, 1, 0);

        //Viene verificato che sia avvenuto il perfezionamento
        Assertions.assertTrue(TestUtils.getRefinement(iun, recIndex, timelineService).isPresent());

        //Viene effettuato il check dei legalFacts generati
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(true)
                .build();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;

        TestUtils.GeneratedLegalFactsPayload generatedLegalFactsPayload = TestUtils.GeneratedLegalFactsPayload.builder()
                .notification(notification)
                .recipient(recipient)
                .recIndex(recIndex)
                .generatedLegalFactsInfo(generatedLegalFactsInfo)
                .endWorkflowStatus(endWorkflowStatus)
                .legalFactGenerator(legalFactGenerator)
                .timelineService(timelineService)
                .sentPecAttemptNumber(sentPecAttemptNumber)
                .delegateInfo(null)
                .build();
        TestUtils.checkGeneratedLegalFacts(generatedLegalFactsPayload);

        //Vengono stampati tutti i legalFacts generati
        String className = this.getClass().getSimpleName();
        TestUtils.writeAllGeneratedLegalFacts(iun, className, timelineService, safeStorageClientMock);

        ConsoleAppenderCustom.checkLogs("Notification is PAID but not VIEWED, should check how!");
    }

    private PnDeliveryPaymentEvent.Payload simulateNotificationPaid(String iun, int recIndex, String timelineIdToWait) {
        if(timelineIdToWait != null) {
            //Viene atteso fino a che la notifica non passa allo stato accettata per simulare il pagamento della notifica
            await().untilAsserted(() ->
                    Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineIdToWait).isPresent())
            );
        }
        PnDeliveryPaymentEvent.Payload paymentEventPayload = PnDeliveryPaymentEvent.Payload.builder()
                .iun(iun)
                .amount(100)
                .creditorTaxId("testCreditorTaxId")
                .noticeCode("testNoticeCode")
                .paymentDate(Instant.now())
                .paymentType(PnDeliveryPaymentEvent.PaymentType.PAGOPA)
                .recipientIdx(recIndex)
                .recipientType(PnDeliveryPaymentEvent.RecipientType.PF)
                .paymentSourceChannel("Internal")
                .uncertainPaymentDate(false)
                .build();
        
        notificationPaidHandler.handleNotificationPaid(paymentEventPayload);
        
        return paymentEventPayload;
    }

}
