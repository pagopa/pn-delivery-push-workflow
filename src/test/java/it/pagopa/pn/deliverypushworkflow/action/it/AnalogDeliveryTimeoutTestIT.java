package it.pagopa.pn.deliverypushworkflow.action.it;

import it.pagopa.pn.deliverypushworkflow.action.it.mockbean.PaperChannelMock;
import it.pagopa.pn.deliverypushworkflow.action.it.mockbean.TimelineClientMock;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.ScheduleRecipientWorkflow;
import it.pagopa.pn.deliverypushworkflow.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.EventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.AnalogFailureWorkflowTimeoutDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogTimeoutDetailsInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.NotificationFeePolicy;
import it.pagopa.pn.deliverypushworkflow.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.paperchannel.PaperChannelPrepareRequest;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.awaitility.Awaitility.await;


public class AnalogDeliveryTimeoutTestIT extends CommonTestConfiguration {

    @MockitoSpyBean
    LegalFactGenerator legalFactGenerator;
    @MockitoSpyBean
    PaperChannelMock paperChannelMock;
    @Autowired
    ScheduleRecipientWorkflow scheduleRecipientWorkflow;
    @Autowired
    TimelineService timelineService;

    private static final int FIRST_ATTEMPT = 0;
    private static final int SECOND_ATTEMPT = 1;

    @Test
    void singleRecipientWithRIRNoDeliveryOutcomes() {
        //Creazione notifica monodestinatario con un workflow analogico con prodotto RIR che non riceve esito
        // al primo tentativo di invio notifica per un tempo X > DATA_TIMEOUT.

        String iun = "IUN123456";
        int recIndex = 0;
        PhysicalAddressInt physicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(PaperChannelMock.EXTCHANNEL_RIR_NO_FEEDBACK)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest")
                .withTaxId("TAXID01")
                .withPhysicalAddress(physicalAddress)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();

        pnDeliveryClientMock.addNotification(notification);

        //Start del workflow
        scheduleRecipientWorkflow.startScheduleRecipientWorkflow(iun);

        //Viene verificato che l'affido sia stato effettuato con prodotto RIR per il primo tentativo
        checkSendRequestedWithRirProduct(iun, recIndex, FIRST_ATTEMPT);

        //Viene effettuato il check della timeline per verificare che sia stato inserito l'elemento di SEND_ANALOG_TIMEOUT_CREATION_REQUEST per il primo tentativo
        await().untilAsserted(() ->
                Assertions.assertTrue(getSendAnalogTimeoutCreationRequestTimelineElement(iun, recIndex, FIRST_ATTEMPT).isPresent())
        );

        //Viene effettuato il check della timeline per verificare che sia stato inserito l'elemento di SEND_ANALOG_TIMEOUT per il primo tentativo
        await().untilAsserted(() ->
                Assertions.assertTrue(getSendAnalogTimeoutTimelineElement(iun, recIndex, FIRST_ATTEMPT).isPresent())
        );

        Instant timeoutDateFirstAttempt = getTimeoutDate(getSendAnalogTimeoutTimelineElement(iun, recIndex, FIRST_ATTEMPT).get());


        //Viene effettuato il check dei legalFacts generati per il primo tentativo
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .deliveryTimeoutLegalFactGenerated(true)
                .build();
        checkLegalFacts(generatedLegalFactsInfo, notification, recipient, recIndex, timeoutDateFirstAttempt, String.valueOf(FIRST_ATTEMPT));

        //Il secondo tentativo di consegna viene effettuato con prodotto RIR e non produce esiti per un tempo
        // X > DATA_TIMEOUT.

        //Viene verificato che l'affido sia stato effettuato con prodotto RIR per il secondo tentativo
        checkSendRequestedWithRirProduct(iun, recIndex, SECOND_ATTEMPT);

        await().untilAsserted(() ->
                Assertions.assertTrue(getSendAnalogTimeoutCreationRequestTimelineElement(iun, recIndex, SECOND_ATTEMPT).isPresent())
        );


        await().untilAsserted(() ->
                Assertions.assertTrue(getSendAnalogTimeoutTimelineElement(iun, recIndex, SECOND_ATTEMPT).isPresent())
        );

        Instant timeoutDateSecondAttempt = getTimeoutDate(getSendAnalogTimeoutTimelineElement(iun, recIndex, SECOND_ATTEMPT).get());

        //Viene effettuato il check dei legalFacts generati per il secondo tentativo
        checkLegalFacts(generatedLegalFactsInfo, notification, recipient, recIndex, timeoutDateSecondAttempt, String.valueOf(SECOND_ATTEMPT));


        //Verifichiamo che sia stato inserito l'elemento di ANALOG_FAILURE_WORKFLOW.
        checkAnalogWorkflowFailureTimeout(iun, recIndex);
        // Verifichiamo che sia stato inserito l'elemento di ANALOG_FAILURE_WORKFLOW con un costo applicato.
        checkAnalogWorkflowFailureTimeoutCost(iun, recIndex, true);

        Mockito.verify(paperChannelMock, Mockito.times(2)).prepare(Mockito.any(PaperChannelPrepareRequest.class));
        Mockito.verify(paperChannelMock, Mockito.times(2)).send(Mockito.any(PaperChannelSendRequest.class));
    }

    @Test
    void singleRecipientWithRIRNoDeliveryOutcomesAndViewBeforeSecondTimeout() {
        //Creazione notifica monodestinatario con un workflow analogico con prodotto RIR che non riceve esito
        // al primo tentativo di invio notifica per un tempo X > DATA_TIMEOUT.

        String iun = "IUN123456";
        int recIndex = 0;
        PhysicalAddressInt physicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(PaperChannelMock.EXTCHANNEL_RIR_NO_FEEDBACK)
                .build();

        //Simulazione visualizzazione notifica prima del secondo tentativo di consegna analogica
        String taxId = TimelineClientMock.SIMULATE_VIEW_NOTIFICATION + TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(recIndex)
                .sentAttemptMade(SECOND_ATTEMPT)
                .build()
        );

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest")
                .withTaxId(taxId)
                .withPhysicalAddress(physicalAddress)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();

        pnDeliveryClientMock.addNotification(notification);

        //Start del workflow
        scheduleRecipientWorkflow.startScheduleRecipientWorkflow(iun);

        //Viene verificato che l'affido sia stato effettuato con prodotto RIR per il primo tentativo
        checkSendRequestedWithRirProduct(iun, recIndex, FIRST_ATTEMPT);

        //Viene effettuato il check della timeline per verificare che sia stato inserito l'elemento di SEND_ANALOG_TIMEOUT_CREATION_REQUEST per il primo tentativo
        await().untilAsserted(() ->
                Assertions.assertTrue(getSendAnalogTimeoutCreationRequestTimelineElement(iun, recIndex, FIRST_ATTEMPT).isPresent())
        );

        //Viene effettuato il check della timeline per verificare che sia stato inserito l'elemento di SEND_ANALOG_TIMEOUT per il primo tentativo
        await().untilAsserted(() ->
                Assertions.assertTrue(getSendAnalogTimeoutTimelineElement(iun, recIndex, FIRST_ATTEMPT).isPresent())
        );

        Instant timeoutDateFirstAttempt = getTimeoutDate(getSendAnalogTimeoutTimelineElement(iun, recIndex, FIRST_ATTEMPT).get());


        //Viene effettuato il check dei legalFacts generati per il primo tentativo
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .deliveryTimeoutLegalFactGenerated(true)
                .build();
        checkLegalFacts(generatedLegalFactsInfo, notification, recipient, recIndex, timeoutDateFirstAttempt, String.valueOf(FIRST_ATTEMPT));

        //Il secondo tentativo di consegna viene effettuato con prodotto RIR e non produce esiti per un tempo
        // X > DATA_TIMEOUT.

        //Viene verificato che l'affido sia stato effettuato con prodotto RIR per il secondo tentativo
        checkSendRequestedWithRirProduct(iun, recIndex, SECOND_ATTEMPT);

        await().untilAsserted(() ->
                Assertions.assertTrue(getSendAnalogTimeoutCreationRequestTimelineElement(iun, recIndex, SECOND_ATTEMPT).isPresent())
        );


        await().untilAsserted(() ->
                Assertions.assertTrue(getSendAnalogTimeoutTimelineElement(iun, recIndex, SECOND_ATTEMPT).isPresent())
        );

        Instant timeoutDateSecondAttempt = getTimeoutDate(getSendAnalogTimeoutTimelineElement(iun, recIndex, SECOND_ATTEMPT).get());

        //Viene effettuato il check dei legalFacts generati per il secondo tentativo
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo2 = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(true)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .deliveryTimeoutLegalFactGenerated(true)
                .build();
        checkLegalFacts(generatedLegalFactsInfo2, notification, recipient, recIndex, timeoutDateSecondAttempt, String.valueOf(SECOND_ATTEMPT));


        //Verifichiamo che sia stato inserito l'elemento di ANALOG_FAILURE_WORKFLOW
        checkAnalogWorkflowFailureTimeout(iun, recIndex);

        //Verifichiamo che il costo della notifica non sia stato inserito nell'elemento di ANALOG_FAILURE_WORKFLOW_TIMEOUT.
        //Dovrebbe essere inserito nell'elemento di visualizzazione.
        checkAnalogWorkflowFailureTimeoutCost(iun, recIndex, false);

        Mockito.verify(paperChannelMock, Mockito.times(2)).prepare(Mockito.any(PaperChannelPrepareRequest.class));
        Mockito.verify(paperChannelMock, Mockito.times(2)).send(Mockito.any(PaperChannelSendRequest.class));
    }

    @Test
    void singleRecipientWithRIRAndDeliveryOutcomes() {
        //Creazione notifica monodestinatario con un workflow analogico con prodotto RIR che riceve esito positivo al primo
        // tentativo di invio notifica.

        String iun = "IUN123456";
        int recIndex = 0;
        PhysicalAddressInt physicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(PaperChannelMock.EXTCHANNEL_RIR_WITH_FEEDBACK_OK)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest")
                .withTaxId("TAXID01")
                .withPhysicalAddress(physicalAddress)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();

        pnDeliveryClientMock.addNotification(notification);

        //Start del workflow
        scheduleRecipientWorkflow.startScheduleRecipientWorkflow(iun);

        //Viene verificato che l'affido sia stato effettuato con prodotto RIR per il primo tentativo
        checkSendRequestedWithRirProduct(iun, recIndex, FIRST_ATTEMPT);

        //Viene effettuato il check della timeline per verificare che sia stato inserito l'elemento di SEND_ANALOG_FEEDBACK per il primo tentativo
        await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(iun, TimelineEventId.SEND_ANALOG_FEEDBACK.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .sentAttemptMade(FIRST_ATTEMPT)
                                .build()
                )).isPresent())
        );

        //Viene effettuato il check della timeline per verificare che siano stati inseriti gli elementi di successo del workflow analogico e perfezionamento
        await().untilAsserted(() -> TestUtils.checkIsPresentAnalogSuccessWorkflowAndRefinement(
                        iun,
                        recIndex,
                        timelineService
                )
        );

        Mockito.verify(paperChannelMock, Mockito.times(1)).prepare(Mockito.any(PaperChannelPrepareRequest.class));
        Mockito.verify(paperChannelMock, Mockito.times(1)).send(Mockito.any(PaperChannelSendRequest.class));
    }

    @Test
    void singleRecipientWithRIRNoDeliveryOutcomesAndPrepareKoForSecondAttempt() {
        /*
        Creazione notifica monodestinatario con un workflow analogico con prodotto RIR che non riceve esito
        al primo tentativo di invio notifica per un tempo X > DATA_TIMEOUT.
        */

        String iun = "IUN123456";
        int recIndex = 0;
        PhysicalAddressInt physicalAddress = PhysicalAddressBuilder.builder()
                .withAddress(PaperChannelMock.EXTCHANNEL_RIR_WITH_PREPARE_KO_SECOND_ATTEMPT)
                .build();

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest")
                .withTaxId("TAXID01")
                .withPhysicalAddress(physicalAddress)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipient(recipient)
                .build();

        pnDeliveryClientMock.addNotification(notification);

        //Start del workflow
        scheduleRecipientWorkflow.startScheduleRecipientWorkflow(iun);

        //Viene verificato che l'affido sia stato effettuato con prodotto RIR per il primo tentativo
        checkSendRequestedWithRirProduct(iun, recIndex, FIRST_ATTEMPT);

        //Viene effettuato il check della timeline per verificare che sia stato inserito l'elemento di SEND_ANALOG_TIMEOUT_CREATION_REQUEST per il primo tentativo
        await().untilAsserted(() ->
                Assertions.assertTrue(getSendAnalogTimeoutCreationRequestTimelineElement(iun, recIndex, FIRST_ATTEMPT).isPresent())
        );

        //Viene effettuato il check della timeline per verificare che sia stato inserito l'elemento di SEND_ANALOG_TIMEOUT per il primo tentativo
        await().untilAsserted(() ->
                Assertions.assertTrue(getSendAnalogTimeoutTimelineElement(iun, recIndex, FIRST_ATTEMPT).isPresent())
        );

        Instant timeoutDateFirstAttempt = getTimeoutDate(getSendAnalogTimeoutTimelineElement(iun, recIndex, FIRST_ATTEMPT).get());


        //Viene effettuato il check dei legalFacts generati per il primo tentativo
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .deliveryTimeoutLegalFactGenerated(true)
                .build();
        checkLegalFacts(generatedLegalFactsInfo, notification, recipient, recIndex, timeoutDateFirstAttempt, String.valueOf(FIRST_ATTEMPT));

        //Il secondo tentativo di consegna fallisce (per assenza di indirizzi) e la notifica deve terminare il suo worfklow
        //con un timeout.
        checkPrepareAnalogFailure(iun, recIndex);
        //Verifichiamo che sia stato inserito l'elemento di ANALOG_FAILURE_WORKFLOW.
        checkAnalogWorkflowFailureTimeout(iun, recIndex);
        // Verifichiamo che sia stato inserito l'elemento di ANALOG_FAILURE_WORKFLOW con un costo applicato.
        checkAnalogWorkflowFailureTimeoutCost(iun, recIndex, true);

        Mockito.verify(paperChannelMock, Mockito.times(2)).prepare(Mockito.any(PaperChannelPrepareRequest.class));
        Mockito.verify(paperChannelMock, Mockito.times(1)).send(Mockito.any(PaperChannelSendRequest.class));
    }

    @Test
    void multiRecipient() {
        /*
        Creazione notifica multidestinatario che deve seguire i seguenti workflow:
            Per il primo destinatario un workflow analogico che riceve esito positivo al primo tentativo di consegna,
            e produce il perfezionamento della notifica per decorrenza termini.

            Per il secondo destinatario un workflow analogico con prodotto RIR che non riceve esito al primo tentativo
            di invio notifica per un tempo X > DATA_TIMEOUT.
            Il secondo tentativo di consegna viene effettuato con prodotto RIR e non produce esiti per un tempo X > DATA_TIMEOUT.
         */

        String iun = "IUN123456";
        int recIndex1 = 0;
        int recIndex2 = 1;
        PhysicalAddressInt physicalAddress1 = PhysicalAddressBuilder.builder()
                .withAddress(PaperChannelMock.EXTCHANNEL_SEND_SUCCESS)
                .build();

        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest1")
                .withTaxId("TAXID01")
                .withPhysicalAddress(physicalAddress1)
                .build();

        PhysicalAddressInt physicalAddress2 = PhysicalAddressBuilder.builder()
                .withAddress(PaperChannelMock.EXTCHANNEL_RIR_NO_FEEDBACK)
                .build();

        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder()
                .withInternalId("internalIdTest2")
                .withTaxId("TAXID02")
                .withPhysicalAddress(physicalAddress2)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withNotificationRecipients(List.of(recipient1, recipient2))
                .build();

        pnDeliveryClientMock.addNotification(notification);

        //Start del workflow
        scheduleRecipientWorkflow.startScheduleRecipientWorkflow(iun);

        //Per il primo destinatario viene effettuato il check della timeline per verificare che sia stato inserito l'elemento
        // di SEND_ANALOG_FEEDBACK per il primo tentativo
        await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(iun, TimelineEventId.SEND_ANALOG_FEEDBACK.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex1)
                                .sentAttemptMade(FIRST_ATTEMPT)
                                .build()
                )).isPresent())
        );

        //Per il primo destinatario viene effettuato il check della timeline per verificare che siano stati inseriti gli
        // elementi di successo del workflow analogico e perfezionamento
        await().untilAsserted(() -> TestUtils.checkIsPresentAnalogSuccessWorkflowAndRefinement(
                        iun,
                        recIndex1,
                        timelineService
                )
        );

        //Per il secondo destinatario viene verificato che l'affido sia stato effettuato con prodotto RIR per il primo tentativo
        checkSendRequestedWithRirProduct(iun, recIndex2, FIRST_ATTEMPT);

        //Verifichiamo sia stato intrapreso il workflow di timeout analogico per il secondo destinatario al primo tentativo di consegna
        await().untilAsserted(() ->
                Assertions.assertTrue(getSendAnalogTimeoutCreationRequestTimelineElement(iun, recIndex2, FIRST_ATTEMPT).isPresent())
        );

        await().untilAsserted(() ->
                Assertions.assertTrue(getSendAnalogTimeoutTimelineElement(iun, recIndex2, FIRST_ATTEMPT).isPresent())
        );

        Instant timeoutDateFirstAttempt = getTimeoutDate(getSendAnalogTimeoutTimelineElement(iun, recIndex2, FIRST_ATTEMPT).get());

        //Viene effettuato il check dei legalFacts generati per il primo tentativo del secondo destinatario
        TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo = TestUtils.GeneratedLegalFactsInfo.builder()
                .notificationReceivedLegalFactGenerated(true)
                .notificationAARGenerated(true)
                .notificationViewedLegalFactGenerated(false)
                .pecDeliveryWorkflowLegalFactsGenerated(false)
                .notificationCompletelyUnreachableLegalFactGenerated(false)
                .deliveryTimeoutLegalFactGenerated(true)
                .build();
        checkLegalFacts(generatedLegalFactsInfo, notification, recipient2, recIndex2, timeoutDateFirstAttempt, String.valueOf(FIRST_ATTEMPT));


        //Verifichiamo sia stato intrapreso il workflow di timeout analogico per il secondo destinatario al secondo tentativo di consegna
        checkSendRequestedWithRirProduct(iun, recIndex2, SECOND_ATTEMPT);

        await().untilAsserted(() ->
                Assertions.assertTrue(getSendAnalogTimeoutCreationRequestTimelineElement(iun, recIndex2, SECOND_ATTEMPT).isPresent())
        );


        await().untilAsserted(() ->
                Assertions.assertTrue(getSendAnalogTimeoutTimelineElement(iun, recIndex2, SECOND_ATTEMPT).isPresent())
        );

        Instant timeoutDateSecondAttempt = getTimeoutDate(getSendAnalogTimeoutTimelineElement(iun, recIndex2, SECOND_ATTEMPT).get());

        //Viene effettuato il check dei legalFacts generati per il secondo tentativo
        checkLegalFacts(generatedLegalFactsInfo, notification, recipient2, recIndex2, timeoutDateSecondAttempt, String.valueOf(SECOND_ATTEMPT));


        //Verifichiamo che sia stato inserito l'elemento di ANALOG_FAILURE_WORKFLOW
        checkAnalogWorkflowFailureTimeout(iun, recIndex2);

        //Verifichiamo che il costo della notifica non sia stato inserito nell'elemento di ANALOG_FAILURE_WORKFLOW_TIMEOUT.
        //Dovrebbe essere inserito nell'elemento di visualizzazione.
        checkAnalogWorkflowFailureTimeoutCost(iun, recIndex2, true);


        Mockito.verify(paperChannelMock, Mockito.times(3)).prepare(Mockito.any(PaperChannelPrepareRequest.class));
        Mockito.verify(paperChannelMock, Mockito.times(3)).send(Mockito.any(PaperChannelSendRequest.class));
    }

    private void checkSendRequestedWithRirProduct(String iun, int recIndex, int sentAttemptMade) {
        String sendAnalogDomicileId = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .sentAttemptMade(sentAttemptMade)
                        .build());

        await().untilAsserted(() -> {
                Optional<TimelineElementInternal> sendAnalogDomicileElement = timelineService.getTimelineElement(iun, sendAnalogDomicileId);
                Assertions.assertTrue(sendAnalogDomicileElement.isPresent());
                Assertions.assertInstanceOf(SendAnalogDetailsInt.class, sendAnalogDomicileElement.get().getDetails(), "Expected SendAnalogTimeoutDetailsInt for detail type");
                SendAnalogDetailsInt detailsInt = (SendAnalogDetailsInt) sendAnalogDomicileElement.get().getDetails();
                Assertions.assertEquals("RIR", detailsInt.getProductType());
        });
    }

    private Optional<TimelineElementInternal> getSendAnalogTimeoutCreationRequestTimelineElement(String iun, int recIndex, int sentAttemptMade) {
        String sendAnalogTimeoutCreationRequestId = TimelineEventId.SEND_ANALOG_TIMEOUT_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .sentAttemptMade(sentAttemptMade)
                        .build()
        );
        return timelineService.getTimelineElement(iun, sendAnalogTimeoutCreationRequestId);
    }

    private Optional<TimelineElementInternal> getSendAnalogTimeoutTimelineElement(String iun, int recIndex, int sentAttemptMade) {
        String sendAnalogTimeoutId = TimelineEventId.SEND_ANALOG_TIMEOUT.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .sentAttemptMade(sentAttemptMade)
                        .build()
        );

        return timelineService.getTimelineElement(iun, sendAnalogTimeoutId);
    }

    private Instant getTimeoutDate(TimelineElementInternal timelineElementInternal) {
        if(timelineElementInternal.getDetails() instanceof SendAnalogTimeoutDetailsInt sendAnalogTimeoutDetailsInt) {
            return sendAnalogTimeoutDetailsInt.getTimeoutDate();
        }

        return null;
    }

    private String buildAnalogFailureTimeoutId(String iun, int recIndex) {
        return TimelineEventId.ANALOG_FAILURE_WORKFLOW_TIMEOUT.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build()
        );
    }

    private void checkAnalogWorkflowFailureTimeout(String iun, int recIndex) {
        String analogFailureTimeoutId = buildAnalogFailureTimeoutId(iun, recIndex);
        await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(iun, analogFailureTimeoutId).isPresent())
        );
    }

    private void checkAnalogWorkflowFailureTimeoutCost(String iun, int recIndex, boolean shouldHaveCost) {
        String analogFailureTimeoutId = buildAnalogFailureTimeoutId(iun, recIndex);
        Optional<TimelineElementInternal> timelineElementInternal = timelineService.getTimelineElement(iun, analogFailureTimeoutId);
        Assertions.assertTrue(timelineElementInternal.isPresent(), "Timeline element for ANALOG_FAILURE_WORKFLOW_TIMEOUT not found");
        if (timelineElementInternal.get().getDetails() instanceof AnalogFailureWorkflowTimeoutDetailsInt analogFailureWorkflowTimeoutDetailsInt) {
            boolean hasCostInDetails = analogFailureWorkflowTimeoutDetailsInt.getNotificationCost() != null && analogFailureWorkflowTimeoutDetailsInt.getNotificationCost() > 0;
            Assertions.assertEquals(shouldHaveCost, hasCostInDetails, "Cost mismatch for ANALOG_FAILURE_WORKFLOW_TIMEOUT");
        } else {
            Assertions.fail("Timeline element details are not of type SendAnalogTimeoutDetailsInt");
        }
    }

    private void checkLegalFacts(TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo, NotificationInt notification, NotificationRecipientInt recipient, int recIndex, Instant timeoutDateFirstAttempt, String sentAttemptMade) {


        TestUtils.GeneratedLegalFactsPayload generatedLegalFactsPayload = TestUtils.GeneratedLegalFactsPayload.builder()
                .notification(notification)
                .recipient(recipient)
                .recIndex(recIndex)
                .generatedLegalFactsInfo(generatedLegalFactsInfo)
                .endWorkflowStatus(EndWorkflowStatus.SUCCESS)
                .legalFactGenerator(legalFactGenerator)
                .timelineService(timelineService)
                .timeoutDate(timeoutDateFirstAttempt)
                .sentAttemptMade(sentAttemptMade)
                .build();
        TestUtils.checkGeneratedLegalFacts(generatedLegalFactsPayload);
    }

    private void checkPrepareAnalogFailure(String iun, int recIndex) {
        String analogDomicileFailureId = TimelineEventId.PREPARE_ANALOG_DOMICILE_FAILURE.buildEventId(
                EventId.builder().iun(iun).recIndex(recIndex).build()
        );
        await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(iun, analogDomicileFailureId).isPresent())
        );
    }
}
