package it.pagopa.pn.deliverypushworkflow.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.action.details.SendCourtesyMessageActionDetails;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypushworkflow.dto.io.IoSendMessageResultInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.EventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.emdintegration.PnEmdIntegrationClient;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypushworkflow.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypushworkflow.service.AddressBookService;
import it.pagopa.pn.deliverypushworkflow.service.ExternalChannelService;
import it.pagopa.pn.deliverypushworkflow.service.IoService;
import it.pagopa.pn.deliverypushworkflow.service.SchedulerService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    private ConfidentialInformationService confidentialInformationService;

    private CourtesyMessageUtils courtesyMessageUtils;

    @BeforeEach
    void setup() {
        addressBookService = Mockito.mock(AddressBookService.class);
        externalChannelService = Mockito.mock(ExternalChannelService.class);
        timelineService = Mockito.mock(TimelineService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
        iOservice = Mockito.mock(IoService.class);
        pnEmdIntegrationClient = mock(PnEmdIntegrationClient.class);
        schedulerService = mock(SchedulerService.class);

        confidentialInformationService = mock(ConfidentialInformationService.class);
        Mockito.lenient().when(confidentialInformationService.savePlannedCourtesyAddress(Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyInt(), Mockito.any(), Mockito.anyString()))
                .thenAnswer(invocation -> Mono.just("COURTESY_PLANNED#" + invocation.getArgument(1) + "#"
                        + invocation.getArgument(2) + "#" + invocation.getArgument(3)));

        courtesyMessageUtils = new CourtesyMessageUtils(addressBookService, confidentialInformationService, timelineService, timelineUtils,
                notificationUtils, schedulerService);
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
    void scheduleCourtesyMessagesActionsAnalogNoChannelsSchedulesAnalogWorkflowImmediately() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Collections.emptyList());

        //WHEN
        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.ANALOG);

        //THEN
        Mockito.verify(schedulerService, never()).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(Instant.class),
                Mockito.eq(ActionType.SEND_COURTESY_MESSAGE_ACTION), Mockito.any(SendCourtesyMessageActionDetails.class));
        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(notification.getIun()), Mockito.eq(0), Mockito.any(Instant.class),
                Mockito.eq(ActionType.ANALOG_WORKFLOW));
        // PROBABLE_SCHEDULING_ANALOG_DATE + SCHEDULE_ANALOG_WORKFLOW
        Mockito.verify(timelineService, times(2)).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
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

    @Test
    void dispatchFreezesOnlyEmailAndSmsAddresses() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL),
                        courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS),
                        courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO),
                        courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP)));

        //WHEN
        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        //THEN
        Mockito.verify(confidentialInformationService).savePlannedCourtesyAddress(recipient.getInternalId(), notification.getIun(), 0,
                CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL, "indirizzo@test.it");
        Mockito.verify(confidentialInformationService).savePlannedCourtesyAddress(recipient.getInternalId(), notification.getIun(), 0,
                CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS, "indirizzo@test.it");
        Mockito.verify(confidentialInformationService, never()).savePlannedCourtesyAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(),
                Mockito.eq(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO), Mockito.anyString());
        Mockito.verify(confidentialInformationService, never()).savePlannedCourtesyAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(),
                Mockito.eq(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP), Mockito.anyString());

        ArgumentCaptor<SendCourtesyMessageActionDetails> detailsCaptor = ArgumentCaptor.forClass(SendCourtesyMessageActionDetails.class);
        Mockito.verify(schedulerService, times(4)).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(Instant.class),
                Mockito.eq(ActionType.SEND_COURTESY_MESSAGE_ACTION), detailsCaptor.capture());

        Map<CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT, String> plannedByChannel = detailsCaptor.getAllValues().stream()
                .collect(HashMap::new, (map, det) -> map.put(det.getChannel(), det.getPlannedAddressId()), HashMap::putAll);

        assertThat(plannedByChannel.get(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL))
                .isEqualTo("COURTESY_PLANNED#" + notification.getIun() + "#0#EMAIL");
        assertThat(plannedByChannel.get(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS))
                .isEqualTo("COURTESY_PLANNED#" + notification.getIun() + "#0#SMS");
        assertThat(plannedByChannel.get(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)).isNull();
        assertThat(plannedByChannel.get(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP)).isNull();
    }

    @Test
    void dispatchSchedulesWithoutPlannedAddressIdWhenFreezeFails() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)));
        Mockito.when(confidentialInformationService.savePlannedCourtesyAddress(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(),
                        Mockito.any(), Mockito.anyString()))
                .thenReturn(Mono.error(new PnInternalException("data vault unreachable", "PN_DELIVERYPUSH_DATAVAULTADDRESSERROR")));

        //WHEN - the dispatch must not fail
        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        //THEN
        ArgumentCaptor<SendCourtesyMessageActionDetails> detailsCaptor = ArgumentCaptor.forClass(SendCourtesyMessageActionDetails.class);
        Mockito.verify(schedulerService).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(Instant.class),
                Mockito.eq(ActionType.SEND_COURTESY_MESSAGE_ACTION), detailsCaptor.capture());

        assertThat(detailsCaptor.getValue().getPlannedAddressId()).isNull();
        assertThat(detailsCaptor.getValue().getChannel()).isEqualTo(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL);
    }

    private static CourtesyDigitalAddressInt courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT type) {
        return CourtesyDigitalAddressInt.builder()
                .type(type)
                .address("indirizzo@test.it")
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
