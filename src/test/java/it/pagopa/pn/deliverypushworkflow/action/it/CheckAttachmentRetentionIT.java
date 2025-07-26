package it.pagopa.pn.deliverypushworkflow.action.it;

import it.pagopa.pn.deliverypushworkflow.action.checkattachmentretention.CheckAttachmentRetentionHandler;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypushworkflow.action.notificationview.NotificationViewedRequestHandler;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.ScheduleRecipientWorkflow;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notificationviewed.NotificationViewedInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.EventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypushworkflow.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypushworkflow.service.SchedulerService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static it.pagopa.pn.deliverypushworkflow.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static org.awaitility.Awaitility.await;

class CheckAttachmentRetentionIT extends CommonTestConfiguration {
    @MockitoSpyBean
    SchedulerService schedulerService;
    @MockitoSpyBean
    CheckAttachmentRetentionHandler checkAttachmentRetentionHandler;
    @Autowired
    ScheduleRecipientWorkflow scheduleRecipientWorkflow;
    @Autowired
    TimelineService timelineService;
    @MockitoSpyBean
    AttachmentUtils attachmentUtils;
    @Autowired
    NotificationViewedRequestHandler notificationViewedRequestHandler;

    @Test
    void verifyCheckAttachmentAlreadyRefined() {
        String iun = TestUtils.getRandomIun();

        String taxId = "taxIdTest";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withIun(iun)
                .withNotificationRecipient(recipient)
                .build();

        pnDeliveryClientMock.addNotification(notification);

        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        final Duration attachmentRetentionTimeAfterValidation = Duration.ofSeconds(10);
        final Duration checkAttachmentTimeBeforeExpiration = Duration.ofSeconds(1);
        TimeParams times = cfg.getTimeParams();
        times.setCheckAttachmentTimeBeforeExpiration(checkAttachmentTimeBeforeExpiration);
        Mockito.when(cfg.getTimeParams()).thenReturn(times);
    //    Mockito.when(cfg.getTimeParams().getCheckAttachmentTimeBeforeExpiration()).thenReturn(Duration.ofSeconds(10));
    //    Mockito.when(cfg.getTimeParams().getAttachmentTimeToAddAfterExpiration()).thenReturn(Duration.ofSeconds(20));

        // Scheduliamo qui il check retention manualmente poichè non è presente in questo dominio la validazione della notifica,
        // in cui in teoria si schedula per la prima volta il check della retention.
        simulateFirstCheckAttachmentRetention(iun, Instant.now().plus(attachmentRetentionTimeAfterValidation));

        //Start del workflow
        scheduleRecipientWorkflow.startScheduleRecipientWorkflow(iun);

        // Viene atteso fino a che non sia stato inserito l'elemento SENDERACK_CREATION_REQUEST
        String timelineId = TimelineEventId.AAR_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(0)
                        .build()
        );
        await().untilAsserted(() -> Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineId).isPresent()));

        //Simulazione visualizzazione della notifica, che comporta il perfezionamento

        notificationViewedRequestHandler.handleViewNotificationDelivery(NotificationViewedInt.builder()
                .iun(iun)
                .recipientIndex(recIndex)
                .viewedDate(Instant.now())
                .build()
        );

        //Si attende fino a che non scada il tempo di scheduling del check attachment
        TimelineElementInternal aarCreationRequest = timelineService.getTimelineElement(iun, timelineId).get();
        Instant dateToWait = aarCreationRequest.getTimestamp().plus(attachmentRetentionTimeAfterValidation.plus(Duration.ofSeconds(5)));
        await()
                .atMost(200, TimeUnit.SECONDS)
                .untilAsserted(() -> Assertions.assertTrue(Instant.now().isAfter(dateToWait)));

        //Viene quindi verificato che il metodo di check attachment sia stato effettivamente richiamato ...
        Mockito.verify(checkAttachmentRetentionHandler).handleCheckAttachmentRetentionBeforeExpiration(Mockito.eq(iun), Mockito.any(Instant.class));
        //... ma che non abbia effettuato l'update della retention perchè la notifica è già perfezionata (per presa visione)
        Mockito.verify(attachmentUtils, Mockito.times(1)).changeAttachmentsRetention(Mockito.eq(notification), Mockito.anyInt());
        // ... e che lo scheduling del check sia dunque avvenuto una sola volta
        Mockito.verify(schedulerService, Mockito.times(1)).scheduleEvent(Mockito.eq(iun), Mockito.any(Instant.class), Mockito.eq(ActionType.CHECK_ATTACHMENT_RETENTION));

        ConsoleAppenderCustom.checkLogs();
    }

    @Test
    void verifyCheckAttachmentNotRefined() {
        String iun = TestUtils.getRandomIun();

        String taxId = "taxIdTest";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_"+taxId)
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();

        String fileDoc = "sha256_doc00";
        List<NotificationDocumentInt> notificationDocumentList = TestUtils.getDocumentList(fileDoc);
        List<TestUtils.DocumentWithContent> listDocumentWithContent = TestUtils.getDocumentWithContents(fileDoc, notificationDocumentList);
        notificationDocumentList = TestUtils.firstFileUploadFromNotification(listDocumentWithContent, notificationDocumentList, safeStorageClientMock);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationDocuments(notificationDocumentList)
                .withIun(iun)
                .withNotificationRecipient(recipient)
                .build();

        pnDeliveryClientMock.addNotification(notification);

        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        final Duration attachmentRetentionTimeAfterValidation = Duration.ofSeconds(2);
        final Duration checkAttachmentTimeBeforeExpiration = Duration.ofSeconds(1);
        final Duration attachmentTimeToAddAfterExpiration = Duration.ofSeconds(20);
        TimeParams times = cfg.getTimeParams();
        times.setCheckAttachmentTimeBeforeExpiration(checkAttachmentTimeBeforeExpiration);
        times.setAttachmentTimeToAddAfterExpiration(attachmentTimeToAddAfterExpiration);
        Mockito.when(cfg.getTimeParams()).thenReturn(times);
        //    Mockito.when(cfg.getTimeParams().getAttachmentTimeToAddAfterExpiration()).thenReturn(Duration.ofSeconds(20));

        // Scheduliamo qui il check retention manualmente poichè non è presente in questo dominio la validazione della notifica,
        // in cui in teoria si schedula per la prima volta il check della retention.
        simulateFirstCheckAttachmentRetention(iun, Instant.now().plus(attachmentRetentionTimeAfterValidation));

        //Start del workflow
        scheduleRecipientWorkflow.startScheduleRecipientWorkflow(iun);

        // Viene atteso fino a che non sia stato inserito l'elemento REFINEMENT
        await().untilAsserted(() -> {
            String timelineId = TimelineEventId.REFINEMENT.buildEventId(
                            EventId.builder()
                                    .iun(iun)
                                    .recIndex(recIndex)
                                    .build()
                    );
            Assertions.assertTrue(timelineService.getTimelineElement(iun, timelineId).isPresent());
        });

        //Si attende fino a che non scada il tempo di scheduling del check attachment
        String timelineId = TimelineEventId.AAR_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(0)
                        .build()
        );
        TimelineElementInternal aarCreationRequest = timelineService.getTimelineElement(iun, timelineId).get();
        Instant dateToWait = aarCreationRequest.getTimestamp().plus(attachmentRetentionTimeAfterValidation.plus(Duration.ofSeconds(5)));
        await()
                .atMost(200, TimeUnit.SECONDS)
                .untilAsserted(() -> Assertions.assertTrue(Instant.now().isAfter(dateToWait)));

        //Viene quindi verificato che il metodo di check attacment sia stato effettivamente richiamato ...
        Mockito.verify(checkAttachmentRetentionHandler).handleCheckAttachmentRetentionBeforeExpiration(Mockito.eq(iun), Mockito.any(Instant.class));
        //... ma che non abbia effettuato l'update della retention perchè la notifica è già perfezionata (per presa visione)
        Mockito.verify(attachmentUtils, Mockito.times(2)).changeAttachmentsRetention(Mockito.eq(notification), Mockito.anyInt());
        //... e che lo scheduling del check retention sia avvenuto una seconda volta
        Mockito.verify(schedulerService, Mockito.times(2)).scheduleEvent(Mockito.eq(iun), Mockito.any(Instant.class), Mockito.eq(ActionType.CHECK_ATTACHMENT_RETENTION));

        ConsoleAppenderCustom.checkLogs();
    }

    private void simulateFirstCheckAttachmentRetention(String iun, Instant checkAttachmentDate) {
        schedulerService.scheduleEvent(iun, checkAttachmentDate, ActionType.CHECK_ATTACHMENT_RETENTION);
    }

}
