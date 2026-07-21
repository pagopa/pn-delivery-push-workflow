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
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.CourtesyChannelFailedDetailsInt;
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
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
    private CourtesyRetryableErrorClassifier retryableErrorClassifier;

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
        retryableErrorClassifier = new CourtesyRetryableErrorClassifier();

        TimeParams timeParams = new TimeParams();
        timeParams.setWaitingForReadCourtesyMessage(Duration.ofDays(5));
        Mockito.lenient().when(mockConfig.getTimeParams()).thenReturn(timeParams);

        courtesyMessageUtils = new CourtesyMessageUtils(addressBookService, externalChannelService,
                timelineService, timelineUtils, notificationUtils, iOservice, mockConfig, pnEmdIntegrationClient,
                new AuditLogServiceImpl(), schedulerService, notificationService, retryableErrorClassifier);
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
        // SEND_COURTESY_MESSAGE + PROBABLE_SCHEDULING_ANALOG_DATE + SCHEDULE_ANALOG_WORKFLOW
        Mockito.verify(timelineService, times(3)).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(notification.getIun()), Mockito.eq(0), Mockito.any(Instant.class),
                Mockito.eq(ActionType.ANALOG_WORKFLOW));
        Mockito.verify(schedulerService, never()).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(Instant.class),
                Mockito.any(ActionType.class), Mockito.any(SendCourtesyMessageActionDetails.class));
    }

    @Test
    void handleSendCourtesyMessageActionAppIoPermanentFailure() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)));

        // NOT_SENT_APPIO_UNAVAILABLE is a permanent outcome -> the channel is closed without retry
        Mockito.when(iOservice.sendIOMessage(Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.any(), Mockito.any()))
                .thenReturn(SendMessageResponse.ResultEnum.NOT_SENT_APPIO_UNAVAILABLE);

        //WHEN
        courtesyMessageUtils.handleSendCourtesyMessageAction(notification.getIun(), 0, details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, DeliveryModeInt.ANALOG));

        //THEN
        Mockito.verify(timelineUtils).buildCourtesyChannelFailedTimelineElement(
                Mockito.eq(0), Mockito.eq(notification),
                Mockito.eq(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO),
                Mockito.eq(DeliveryModeInt.ANALOG), Mockito.anyString());
        Mockito.verify(timelineService, times(1)).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
        Mockito.verify(schedulerService, never()).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(Instant.class),
                Mockito.any(ActionType.class), Mockito.any(SendCourtesyMessageActionDetails.class));
    }

    @Test
    void handleSendCourtesyMessageActionAppIoRetryableErrorReschedulesWithBackoff() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)));

        // ERROR_USER_STATUS is a transient technical error -> classified as retryable
        Mockito.when(iOservice.sendIOMessage(Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.any(), Mockito.any()))
                .thenReturn(SendMessageResponse.ResultEnum.ERROR_USER_STATUS);
        configureRetryIntervals(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, List.of(5, 10, 20, 40));

        //WHEN - the first send (retryIndex=0) fails
        Instant beforeCall = Instant.now();
        courtesyMessageUtils.handleSendCourtesyMessageAction(notification.getIun(), 0, details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, DeliveryModeInt.ANALOG));
        Instant afterCall = Instant.now();

        //THEN - the same action is rescheduled with retryIndex=1 at now + 5 minutes (intervals[0]); the channel is not closed
        ArgumentCaptor<SendCourtesyMessageActionDetails> detailsCaptor = ArgumentCaptor.forClass(SendCourtesyMessageActionDetails.class);
        ArgumentCaptor<Instant> dateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(notification.getIun()), Mockito.eq(0), dateCaptor.capture(),
                Mockito.eq(ActionType.SEND_COURTESY_MESSAGE_ACTION), detailsCaptor.capture());

        SendCourtesyMessageActionDetails rescheduled = detailsCaptor.getValue();
        assertThat(rescheduled.getChannel()).isEqualTo(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO);
        assertThat(rescheduled.getRetryIndex()).isEqualTo(1);
        assertThat(rescheduled.getDeliveryMode()).isEqualTo(DeliveryModeInt.ANALOG);
        assertThat(dateCaptor.getValue()).isBetween(beforeCall.plus(Duration.ofMinutes(5)), afterCall.plus(Duration.ofMinutes(5)));

        Mockito.verify(timelineUtils, never()).buildCourtesyChannelFailedTimelineElement(
                Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString());
        Mockito.verify(timelineService, never()).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    void handleSendCourtesyMessageActionAppIoRetryableErrorIntervalsExhaustedClosesChannel() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)));

        Mockito.when(iOservice.sendIOMessage(Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.any(), Mockito.any()))
                .thenReturn(SendMessageResponse.ResultEnum.ERROR_USER_STATUS);
        configureRetryIntervals(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, List.of(5, 10, 20, 40));

        //WHEN - the last configured retry (retryIndex=4, equal to the list size) fails: intervals are exhausted
        courtesyMessageUtils.handleSendCourtesyMessageAction(notification.getIun(), 0, details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, DeliveryModeInt.ANALOG, 4));

        //THEN - no further scheduling, the channel is closed
        Mockito.verify(timelineUtils).buildCourtesyChannelFailedTimelineElement(
                Mockito.eq(0), Mockito.eq(notification),
                Mockito.eq(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO),
                Mockito.eq(DeliveryModeInt.ANALOG), Mockito.anyString());
        Mockito.verify(timelineService, times(1)).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
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
        // SEND_COURTESY_MESSAGE + PROBABLE_SCHEDULING_ANALOG_DATE + SCHEDULE_ANALOG_WORKFLOW
        Mockito.verify(timelineService, times(3)).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(notification.getIun()), Mockito.eq(0), Mockito.any(Instant.class),
                Mockito.eq(ActionType.ANALOG_WORKFLOW));
    }

    @Test
    void handleSendCourtesyMessageActionTppNoChannelsEnabledPermanent() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP)));

        // NO_CHANNELS_ENABLED is a permanent outcome -> the channel is closed without retry
        final it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponse tppResponse =
                new it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponse();
        tppResponse.setOutcome(it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponse.OutcomeEnum.NO_CHANNELS_ENABLED);
        Mockito.when(pnEmdIntegrationClient.sendMessage(Mockito.any(SendMessageRequestBody.class))).thenReturn(tppResponse);

        //WHEN
        courtesyMessageUtils.handleSendCourtesyMessageAction(notification.getIun(), 0, details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, DeliveryModeInt.ANALOG));

        //THEN
        Mockito.verify(timelineUtils).buildCourtesyChannelFailedTimelineElement(
                Mockito.eq(0), Mockito.eq(notification),
                Mockito.eq(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP),
                Mockito.eq(DeliveryModeInt.ANALOG), Mockito.anyString());
        Mockito.verify(timelineService, times(1)).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    void handleSendCourtesyMessageActionTppRetryableErrorReschedulesWithBackoff() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP)));

        // an HTTP 500 is a transient error -> classified as retryable; no exception escapes
        Mockito.when(pnEmdIntegrationClient.sendMessage(Mockito.any(SendMessageRequestBody.class)))
                .thenThrow(WebClientResponseException.create(500, "Internal Server Error", HttpHeaders.EMPTY, new byte[0], null));
        configureRetryIntervals(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, List.of(2, 4, 8));

        //WHEN - the second attempt (retryIndex=1) fails
        Instant beforeCall = Instant.now();
        courtesyMessageUtils.handleSendCourtesyMessageAction(notification.getIun(), 0, details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, DeliveryModeInt.ANALOG, 1));
        Instant afterCall = Instant.now();

        //THEN - rescheduled with retryIndex=2 at now + 4 minutes (intervals[1]); the channel is not closed
        ArgumentCaptor<SendCourtesyMessageActionDetails> detailsCaptor = ArgumentCaptor.forClass(SendCourtesyMessageActionDetails.class);
        ArgumentCaptor<Instant> dateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(notification.getIun()), Mockito.eq(0), dateCaptor.capture(),
                Mockito.eq(ActionType.SEND_COURTESY_MESSAGE_ACTION), detailsCaptor.capture());

        SendCourtesyMessageActionDetails rescheduled = detailsCaptor.getValue();
        assertThat(rescheduled.getChannel()).isEqualTo(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP);
        assertThat(rescheduled.getRetryIndex()).isEqualTo(2);
        assertThat(dateCaptor.getValue()).isBetween(beforeCall.plus(Duration.ofMinutes(4)), afterCall.plus(Duration.ofMinutes(4)));

        Mockito.verify(timelineUtils, never()).buildCourtesyChannelFailedTimelineElement(
                Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString());
        Mockito.verify(timelineService, never()).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    void handleSendCourtesyMessageActionTppRetryableErrorEmptyIntervalsClosesChannel() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP)));

        Mockito.when(pnEmdIntegrationClient.sendMessage(Mockito.any(SendMessageRequestBody.class)))
                .thenThrow(WebClientResponseException.create(500, "Internal Server Error", HttpHeaders.EMPTY, new byte[0], null));
        // empty interval list -> no retry for this channel, today's behaviour
        configureRetryIntervals(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, List.of());

        //WHEN
        courtesyMessageUtils.handleSendCourtesyMessageAction(notification.getIun(), 0, details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, DeliveryModeInt.ANALOG));

        //THEN
        Mockito.verify(timelineUtils).buildCourtesyChannelFailedTimelineElement(
                Mockito.eq(0), Mockito.eq(notification),
                Mockito.eq(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP),
                Mockito.eq(DeliveryModeInt.ANALOG), Mockito.anyString());
        Mockito.verify(timelineService, times(1)).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
        Mockito.verify(schedulerService, never()).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(Instant.class),
                Mockito.any(ActionType.class), Mockito.any(SendCourtesyMessageActionDetails.class));
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
        // only SEND_COURTESY_MESSAGE: the DIGITAL branch does not schedule the analog workflow
        Mockito.verify(timelineService, times(1)).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
        Mockito.verify(schedulerService, never()).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(Instant.class),
                Mockito.eq(ActionType.ANALOG_WORKFLOW));
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
    void handleSendCourtesyMessageActionAnalogAllChannelsClosedWithoutSuccessSchedulesAnalogWorkflow() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);
        String iun = notification.getIun();

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP)));
        Mockito.when(pnEmdIntegrationClient.sendMessage(Mockito.any(SendMessageRequestBody.class))).thenReturn(tppPermanentFailure());

        // the just-written COURTESY_CHANNEL_FAILED is visible via strongly consistent read; no delivery exists
        Mockito.when(timelineService.getTimelineElementStrongly(iun, courtesyChannelFailedId(iun, CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP)))
                .thenReturn(Optional.of(courtesyChannelFailedElement(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP)));

        //WHEN
        courtesyMessageUtils.handleSendCourtesyMessageAction(iun, 0, details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, DeliveryModeInt.ANALOG));

        //THEN
        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(iun), Mockito.eq(0), Mockito.any(Instant.class), Mockito.eq(ActionType.ANALOG_WORKFLOW));
    }

    @Test
    void handleSendCourtesyMessageActionAnalogChannelStillOpenDoesNotScheduleAnalogWorkflow() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);
        String iun = notification.getIun();

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP),
                        courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS)));
        Mockito.when(pnEmdIntegrationClient.sendMessage(Mockito.any(SendMessageRequestBody.class))).thenReturn(tppPermanentFailure());

        // only TPP is closed; SMS has no outcome yet
        Mockito.when(timelineService.getTimelineElementStrongly(iun, courtesyChannelFailedId(iun, CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP)))
                .thenReturn(Optional.of(courtesyChannelFailedElement(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP)));

        //WHEN
        courtesyMessageUtils.handleSendCourtesyMessageAction(iun, 0, details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, DeliveryModeInt.ANALOG));

        //THEN
        Mockito.verify(schedulerService, never()).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(Instant.class), Mockito.eq(ActionType.ANALOG_WORKFLOW));
    }

    @Test
    void handleSendCourtesyMessageActionAnalogSuccessOnAnotherChannelTakesPrecedence() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);
        String iun = notification.getIun();

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(List.of(courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP),
                        courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)));
        Mockito.when(pnEmdIntegrationClient.sendMessage(Mockito.any(SendMessageRequestBody.class))).thenReturn(tppPermanentFailure());

        // TPP is closed without success, but EMAIL already delivered: the +waiting scheduling takes precedence
        Mockito.when(timelineService.getTimelineElementStrongly(iun, courtesyChannelFailedId(iun, CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP)))
                .thenReturn(Optional.of(courtesyChannelFailedElement(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP)));
        String emailDeliveredId = CourtesyMessageUtils.getSendCourtesyTimelineElementId(0, iun, CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL, Boolean.FALSE);
        Mockito.when(timelineService.getTimelineElementStrongly(iun, emailDeliveredId)).thenReturn(Optional.of(new TimelineElementInternal()));

        //WHEN
        courtesyMessageUtils.handleSendCourtesyMessageAction(iun, 0, details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, DeliveryModeInt.ANALOG));

        //THEN
        Mockito.verify(schedulerService, never()).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(Instant.class), Mockito.eq(ActionType.ANALOG_WORKFLOW));
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

    private static String courtesyChannelFailedId(String iun, CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT channel) {
        return TimelineEventId.COURTESY_CHANNEL_FAILED.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(0)
                .courtesyAddressType(channel)
                .build());
    }

    private static TimelineElementInternal courtesyChannelFailedElement(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT channel) {
        return TimelineElementInternal.builder()
                .details(CourtesyChannelFailedDetailsInt.builder()
                        .channelType(channel)
                        .deliveryMode(DeliveryModeInt.ANALOG)
                        .build())
                .build();
    }

    private static it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponse tppPermanentFailure() {
        var response = new it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponse();
        response.setOutcome(it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponse.OutcomeEnum.NO_CHANNELS_ENABLED);
        return response;
    }

    private static CourtesyDigitalAddressInt courtesyAddress(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT type) {
        return CourtesyDigitalAddressInt.builder()
                .type(type)
                .address("indirizzo@test.it")
                .build();
    }

    private static SendCourtesyMessageActionDetails details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT channel, DeliveryModeInt deliveryMode) {
        return details(channel, deliveryMode, 0);
    }

    private static SendCourtesyMessageActionDetails details(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT channel, DeliveryModeInt deliveryMode, int retryIndex) {
        return SendCourtesyMessageActionDetails.builder()
                .channel(channel)
                .retryIndex(retryIndex)
                .deliveryMode(deliveryMode)
                .build();
    }

    private void configureRetryIntervals(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT channel, List<Integer> minutes) {
        PnDeliveryPushWorkflowConfigs.CourtesyRetry.IntervalsMinutes intervalsMinutes = new PnDeliveryPushWorkflowConfigs.CourtesyRetry.IntervalsMinutes();
        switch (channel) {
            case EMAIL -> intervalsMinutes.setEmail(minutes);
            case SMS -> intervalsMinutes.setSms(minutes);
            case APPIO -> intervalsMinutes.setIo(minutes);
            case TPP -> intervalsMinutes.setTpp(minutes);
        }
        PnDeliveryPushWorkflowConfigs.CourtesyRetry courtesyRetry = new PnDeliveryPushWorkflowConfigs.CourtesyRetry();
        courtesyRetry.setIntervalsMinutes(intervalsMinutes);
        Mockito.when(mockConfig.getCourtesyRetry()).thenReturn(courtesyRetry);
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
