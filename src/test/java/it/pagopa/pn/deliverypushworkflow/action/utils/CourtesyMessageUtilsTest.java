package it.pagopa.pn.deliverypushworkflow.action.utils;

import it.pagopa.pn.deliverypushworkflow.action.details.SendCourtesyMessageActionDetails;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypushworkflow.dto.io.IoSendMessageResultInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.EventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageRequestBody;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry.model.SendMessageResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.emdintegration.PnEmdIntegrationClient;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypushworkflow.service.AddressBookService;
import it.pagopa.pn.deliverypushworkflow.service.ExternalChannelService;
import it.pagopa.pn.deliverypushworkflow.service.IoService;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.SchedulerService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import it.pagopa.pn.deliverypushworkflow.service.impl.AuditLogServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static it.pagopa.pn.deliverypushworkflow.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CourtesyMessageUtilsTest {

    private AddressBookService addressBookService;
    private ExternalChannelService externalChannelService;
    private TimelineService timelineService;
    private TimelineUtils timelineUtils;
    private NotificationUtils notificationUtils;
    private IoService iOservice;
    private PnEmdIntegrationClient pnEmdIntegrationClient;
    private SchedulerService schedulerService;
    private NotificationService notificationService;

    private CourtesyMessageUtils courtesyMessageUtils;
    private PnDeliveryPushWorkflowConfigs mockConfig;

    @BeforeEach
    void setup() {
        addressBookService = Mockito.mock(AddressBookService.class);
        externalChannelService = Mockito.mock(ExternalChannelService.class);
        timelineService = Mockito.mock(TimelineService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
        iOservice = Mockito.mock(IoService.class);
        mockConfig = mock(PnDeliveryPushWorkflowConfigs.class);
        pnEmdIntegrationClient = mock(PnEmdIntegrationClient.class);
        schedulerService = mock(SchedulerService.class);
        notificationService = mock(NotificationService.class);

        TimeParams timeParams = new TimeParams();
        timeParams.setWaitingForReadCourtesyMessage(Duration.ofDays(5));
        Mockito.lenient().when(mockConfig.getTimeParams()).thenReturn(timeParams);

        courtesyMessageUtils = new CourtesyMessageUtils(addressBookService, externalChannelService,
                timelineService, timelineUtils, notificationUtils, iOservice, mockConfig, pnEmdIntegrationClient,
                new AuditLogServiceImpl(), schedulerService, notificationService);
    }

    @Test
    void scheduleCourtesyMessagesActionsSchedulesOneActionPerChannel() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        CourtesyDigitalAddressInt appIo = courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO);
        CourtesyDigitalAddressInt sms = courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(appIo, sms));

        //WHEN
        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        //THEN
        ArgumentCaptor<SendCourtesyMessageActionDetails> detailsCaptor = ArgumentCaptor.forClass(SendCourtesyMessageActionDetails.class);
        Mockito.verify(schedulerService, times(2)).scheduleEvent(
                Mockito.eq(notification.getIun()), Mockito.eq(0), Mockito.any(Instant.class),
                Mockito.eq(ActionType.SEND_COURTESY_MESSAGE_ACTION), detailsCaptor.capture());

        List<SendCourtesyMessageActionDetails> allDetails = detailsCaptor.getAllValues();
        assertThat(allDetails).extracting(SendCourtesyMessageActionDetails::getChannel)
                .containsExactlyInAnyOrder(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO,
                        CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS);
        assertThat(allDetails).allMatch(d -> d.getRetryIndex() == 0);
        assertThat(allDetails).allMatch(d -> d.getDeliveryMode() == DeliveryModeInt.DIGITAL);
        //Non viene inviato nulla in modo sincrono
        Mockito.verifyNoInteractions(iOservice, externalChannelService, pnEmdIntegrationClient);
    }

    @Test
    void handleSendCourtesyMessageActionAppIoSuccess() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)));

        final SendMessageResponse.ResultEnum sentCourtesy = SendMessageResponse.ResultEnum.SENT_COURTESY;
        Mockito.when(iOservice.sendIOMessage(Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.any(), Mockito.any())).thenReturn(sentCourtesy);

        //WHEN
        courtesyMessageUtils.handleSendCourtesyMessageAction(notification.getIun(), 0, details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, DeliveryModeInt.ANALOG));

        //THEN
        IoSendMessageResultInt sendMessageResultInt = IoSendMessageResultInt.valueOf(sentCourtesy.getValue());
        Mockito.verify(timelineUtils).buildSendCourtesyMessageTimelineElement(
                Mockito.eq(0), Mockito.eq(notification), Mockito.any(CourtesyDigitalAddressInt.class), Mockito.any(Instant.class),
                Mockito.anyString(), Mockito.eq(sendMessageResultInt));
        // SEND_COURTESY_MESSAGE + PROBABLE_SCHEDULING_ANALOG_DATE
        Mockito.verify(timelineService, times(2)).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
        // nessuna riprogrammazione in WI-1.2
        Mockito.verify(schedulerService, never()).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(Instant.class),
                Mockito.any(ActionType.class), Mockito.any(SendCourtesyMessageActionDetails.class));
    }

    @Test
    void handleSendCourtesyMessageActionAppIoNotSent() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)));

        Mockito.when(iOservice.sendIOMessage(Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.any(), Mockito.any()))
                .thenReturn(SendMessageResponse.ResultEnum.ERROR_USER_STATUS);

        //WHEN
        courtesyMessageUtils.handleSendCourtesyMessageAction(notification.getIun(), 0, details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, DeliveryModeInt.ANALOG));

        //THEN
        Mockito.verify(timelineService, never()).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
        Mockito.verify(schedulerService, never()).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(Instant.class),
                Mockito.any(ActionType.class), Mockito.any(SendCourtesyMessageActionDetails.class));
    }

    @Test
    void handleSendCourtesyMessageActionTppSuccess() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP)));

        final it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponse tppResponse =
                new it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponse();
        tppResponse.setOutcome(it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponse.OutcomeEnum.OK);
        Mockito.when(pnEmdIntegrationClient.sendMessage(Mockito.any(SendMessageRequestBody.class))).thenReturn(tppResponse);

        //WHEN
        courtesyMessageUtils.handleSendCourtesyMessageAction(notification.getIun(), 0, details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, DeliveryModeInt.ANALOG));

        //THEN
        Mockito.verify(pnEmdIntegrationClient).sendMessage(Mockito.any(SendMessageRequestBody.class));
        // SEND_COURTESY_MESSAGE + PROBABLE_SCHEDULING_ANALOG_DATE
        Mockito.verify(timelineService, times(2)).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    void handleSendCourtesyMessageActionEmailSuccess() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)));

        //WHEN
        courtesyMessageUtils.handleSendCourtesyMessageAction(notification.getIun(), 0, details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL, DeliveryModeInt.DIGITAL));

        //THEN
        Mockito.verify(externalChannelService).sendCourtesyNotification(Mockito.eq(notification), Mockito.any(CourtesyDigitalAddressInt.class),
                Mockito.eq(0), Mockito.anyString(), Mockito.eq(DeliveryModeInt.DIGITAL));
        // SEND_COURTESY_MESSAGE + PROBABLE_SCHEDULING_ANALOG_DATE
        Mockito.verify(timelineService, times(2)).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    void handleSendCourtesyMessageActionAddressNotFound() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        // il canale richiesto (TPP) non è tra gli indirizzi disponibili
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)));

        //WHEN
        courtesyMessageUtils.handleSendCourtesyMessageAction(notification.getIun(), 0, details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, DeliveryModeInt.ANALOG));

        //THEN
        Mockito.verifyNoInteractions(iOservice, externalChannelService, pnEmdIntegrationClient);
        Mockito.verify(timelineService, never()).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    void handleSendCourtesyMessageActionNotificationCancelled() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)));
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(notification.getIun())).thenReturn(true);

        //WHEN
        courtesyMessageUtils.handleSendCourtesyMessageAction(notification.getIun(), 0, details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, DeliveryModeInt.ANALOG));

        //THEN
        Mockito.verifyNoInteractions(iOservice, externalChannelService, pnEmdIntegrationClient);
        Mockito.verify(timelineService, never()).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    void addSendCourtesyMessageToTimeline() {
        // GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);
        CourtesyDigitalAddressInt courtesyDigitalAddressInt = CourtesyDigitalAddressInt.builder()
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)
                .build();
        Instant instant = Instant.now();

        ArgumentCaptor<String> eventIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

        // WHEN
        courtesyMessageUtils.addSendCourtesyMessageToTimeline(notification, 0, courtesyDigitalAddressInt, instant);

        // THEN
        Mockito.verify(timelineUtils, Mockito.times(1)).buildSendCourtesyMessageTimelineElement(
                Mockito.anyInt(), Mockito.any(NotificationInt.class), Mockito.any(CourtesyDigitalAddressInt.class), Mockito.any(),
                eventIdArgumentCaptor.capture(), Mockito.any(IoSendMessageResultInt.class));

        String firstEventIdInTimeline = eventIdArgumentCaptor.getAllValues().getFirst();

        String firstEventIdExpected = TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(EventId.builder()
                .iun(notification.getIun())
                .recIndex(0)
                .courtesyAddressType(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)
                .build()
        );

        Assertions.assertEquals(firstEventIdExpected, firstEventIdInTimeline);
    }

    private static CourtesyDigitalAddressInt courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT type) {
        return CourtesyDigitalAddressInt.builder()
                .type(type)
                .address("indirizzo@test.it")
                .build();
    }

    private static SendCourtesyMessageActionDetails details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT channel, DeliveryModeInt deliveryMode) {
        return SendCourtesyMessageActionDetails.builder()
                .channel(channel)
                .retryIndex(0)
                .deliveryMode(deliveryMode)
                .build();
    }

    private static NotificationInt getNotificationInt(NotificationRecipientInt recipient) {
        return NotificationTestBuilder.builder()
                .withIun("iun_01")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();
    }

    private static NotificationRecipientInt getNotificationRecipientInt() {
        String taxId = "TaxId";
        return NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_" + taxId)
                .withDigitalDomicile(
                        LegalDigitalAddressInt.builder()
                                .address("address")
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .build()
                )
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .withPayments(Collections.emptyList())
                .build();
    }
}
