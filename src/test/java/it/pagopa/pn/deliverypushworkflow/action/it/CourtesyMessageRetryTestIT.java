package it.pagopa.pn.deliverypushworkflow.action.it;

import it.pagopa.pn.deliverypushworkflow.action.it.mockbean.PnEmdIntegrationClientMock;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.EventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageRequestBody;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry.model.SendMessageRequest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry.model.SendMessageResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import it.pagopa.pn.deliverypushworkflow.service.impl.ExternalChannelServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.awaitility.Awaitility.await;

/**
 * Integration tests for the courtesy long-retry flow. The whole loop is exercised end-to-end through the real action
 * pool: the dispatcher schedules a {@code SEND_COURTESY_MESSAGE_ACTION}, the pool re-dispatches it to the handler, and
 * on a retryable error the same action is rescheduled with the next retry index and the per-channel backoff interval.
 * Per-channel intervals are configured to a single immediate retry ({@code [0]}). Each channel is covered for the three
 * outcomes (OK, retryable error, permanent error);
 */
class CourtesyMessageRetryTestIT extends CommonTestConfiguration {

    private static final String PA_ID = "paId01";
    private static final String INTERNAL_ID = "ANON_testTaxId";

    @Autowired
    PnExternalRegistryClient pnExternalRegistryClient;

    @MockitoSpyBean
    PnEmdIntegrationClientMock pnEmdIntegrationClientMock;

    @MockitoSpyBean
    ExternalChannelServiceImpl externalChannelService;

    @Autowired
    CourtesyMessageUtils courtesyMessageUtils;

    @Autowired
    TimelineService timelineService;

    // ============================ App IO ============================

    @Test
    void appIoOkDelivers() {
        NotificationInt notification = prepareNotification(COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO);
        configureSingleRetry(COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO);
        stubIo().thenReturn(ioResult(SendMessageResponse.ResultEnum.SENT_COURTESY));

        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        String iun = notification.getIun();
        awaitPresent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, true);
        Mockito.verify(pnExternalRegistryClient, Mockito.times(1)).sendIOMessage(Mockito.any(SendMessageRequest.class));
        assertAbsent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, false);
    }

    @Test
    void appIoRetryableResultThenSuccessDelivers() {
        NotificationInt notification = prepareNotification(COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO);
        configureSingleRetry(COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO);
        stubIo().thenReturn(ioResult(SendMessageResponse.ResultEnum.ERROR_USER_STATUS))
                .thenReturn(ioResult(SendMessageResponse.ResultEnum.SENT_COURTESY));

        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        String iun = notification.getIun();
        awaitPresent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, true);
        Mockito.verify(pnExternalRegistryClient, Mockito.times(2)).sendIOMessage(Mockito.any(SendMessageRequest.class));
        assertAbsent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, false);
    }

    @Test
    void appIoPermanentResultClosesChannel() {
        NotificationInt notification = prepareNotification(COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO);
        configureSingleRetry(COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO);
        stubIo().thenReturn(ioResult(SendMessageResponse.ResultEnum.NOT_SENT_APPIO_UNAVAILABLE));

        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        String iun = notification.getIun();
        awaitPresent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, false);
        Mockito.verify(pnExternalRegistryClient, Mockito.times(1)).sendIOMessage(Mockito.any(SendMessageRequest.class));
        assertAbsent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, true);
    }

    @Test
    void appIoRetryableTransportErrorThenSuccessDelivers() {
        NotificationInt notification = prepareNotification(COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO);
        configureSingleRetry(COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO);
        stubIo().thenThrow(transientError())
                .thenReturn(ioResult(SendMessageResponse.ResultEnum.SENT_COURTESY));

        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        String iun = notification.getIun();
        awaitPresent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, true);
        Mockito.verify(pnExternalRegistryClient, Mockito.times(2)).sendIOMessage(Mockito.any(SendMessageRequest.class));
        assertAbsent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, false);
    }

    @Test
    void appIoPermanentTransportErrorClosesChannel() {
        NotificationInt notification = prepareNotification(COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO);
        configureSingleRetry(COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO);
        stubIo().thenThrow(permanentError());

        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        String iun = notification.getIun();
        awaitPresent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, false);
        Mockito.verify(pnExternalRegistryClient, Mockito.times(1)).sendIOMessage(Mockito.any(SendMessageRequest.class));
        assertAbsent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, true);
    }

    @Test
    void appIoRetryableErrorExhaustsRetriesAndClosesChannel() {
        NotificationInt notification = prepareNotification(COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO);
        configureSingleRetry(COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO);
        // the transient result persists across every attempt: initial send + one retry, then intervals are exhausted
        stubIo().thenReturn(ioResult(SendMessageResponse.ResultEnum.ERROR_USER_STATUS));

        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        String iun = notification.getIun();
        awaitPresent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, false);
        Mockito.verify(pnExternalRegistryClient, Mockito.times(2)).sendIOMessage(Mockito.any(SendMessageRequest.class));
        assertAbsent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, true);
    }

    // ============================ TPP ============================
    // TPP is always injected among the courtesy channels

    @Test
    void tppOkDelivers() {
        NotificationInt notification = prepareNotification();
        configureSingleRetry(COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP);
        Mockito.doReturn(tppResponse(true)).when(pnEmdIntegrationClientMock).sendMessage(Mockito.any(SendMessageRequestBody.class));

        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        String iun = notification.getIun();
        awaitPresent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, true);
        Mockito.verify(pnEmdIntegrationClientMock, Mockito.times(1)).sendMessage(Mockito.any(SendMessageRequestBody.class));
        assertAbsent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, false);
    }

    @Test
    void tppRetryableTransportErrorThenSuccessDelivers() {
        NotificationInt notification = prepareNotification();
        configureSingleRetry(COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP);
        Mockito.doThrow(transientError()).doReturn(tppResponse(true))
                .when(pnEmdIntegrationClientMock).sendMessage(Mockito.any(SendMessageRequestBody.class));

        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        String iun = notification.getIun();
        awaitPresent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, true);
        Mockito.verify(pnEmdIntegrationClientMock, Mockito.times(2)).sendMessage(Mockito.any(SendMessageRequestBody.class));
        assertAbsent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, false);
    }

    @Test
    void tppPermanentNoChannelsEnabledClosesChannel() {
        NotificationInt notification = prepareNotification();
        configureSingleRetry(COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP);
        Mockito.doReturn(tppResponse(false)).when(pnEmdIntegrationClientMock).sendMessage(Mockito.any(SendMessageRequestBody.class));

        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        String iun = notification.getIun();
        awaitPresent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, false);
        Mockito.verify(pnEmdIntegrationClientMock, Mockito.times(1)).sendMessage(Mockito.any(SendMessageRequestBody.class));
        assertAbsent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, true);
    }

    @Test
    void tppRetryableTransportErrorExhaustsRetriesAndClosesChannel() {
        NotificationInt notification = prepareNotification();
        configureSingleRetry(COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP);
        // the 5xx persists across every attempt: initial send + one retry, then intervals are exhausted
        Mockito.doThrow(transientError()).when(pnEmdIntegrationClientMock).sendMessage(Mockito.any(SendMessageRequestBody.class));

        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        String iun = notification.getIun();
        awaitPresent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, false);
        Mockito.verify(pnEmdIntegrationClientMock, Mockito.times(2)).sendMessage(Mockito.any(SendMessageRequestBody.class));
        assertAbsent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, true);
    }

    // ============================ EMAIL / SMS ============================

    @ParameterizedTest
    @EnumSource(value = COURTESY_DIGITAL_ADDRESS_TYPE_INT.class, names = {"EMAIL", "SMS"})
    void externalChannelOkDelivers(COURTESY_DIGITAL_ADDRESS_TYPE_INT channel) {
        NotificationInt notification = prepareNotification(channel);
        configureSingleRetry(channel);
        Mockito.reset(externalChannelService);
        applyToExternalChannelSend(Mockito.doNothing());

        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        String iun = notification.getIun();
        awaitPresent(iun, channel, true);
        verifyExternalChannelSend(1);
        assertAbsent(iun, channel, false);
    }

    @ParameterizedTest
    @EnumSource(value = COURTESY_DIGITAL_ADDRESS_TYPE_INT.class, names = {"EMAIL", "SMS"})
    void externalChannelRetryableErrorThenSuccessDelivers(COURTESY_DIGITAL_ADDRESS_TYPE_INT channel) {
        NotificationInt notification = prepareNotification(channel);
        configureSingleRetry(channel);
        Mockito.reset(externalChannelService);
        applyToExternalChannelSend(Mockito.doThrow(transientError()).doNothing());

        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        String iun = notification.getIun();
        awaitPresent(iun, channel, true);
        verifyExternalChannelSend(2);
        assertAbsent(iun, channel, false);
    }

    @ParameterizedTest
    @EnumSource(value = COURTESY_DIGITAL_ADDRESS_TYPE_INT.class, names = {"EMAIL", "SMS"})
    void externalChannelRetryableErrorExhaustsRetriesAndClosesChannel(COURTESY_DIGITAL_ADDRESS_TYPE_INT channel) {
        NotificationInt notification = prepareNotification(channel);
        configureSingleRetry(channel);
        Mockito.reset(externalChannelService);
        // the 5xx persists across every attempt: initial send + one retry, then intervals are exhausted
        applyToExternalChannelSend(Mockito.doThrow(transientError()));

        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        String iun = notification.getIun();
        awaitPresent(iun, channel, false);
        verifyExternalChannelSend(2);
        assertAbsent(iun, channel, true);
    }

    @ParameterizedTest
    @EnumSource(value = COURTESY_DIGITAL_ADDRESS_TYPE_INT.class, names = {"EMAIL", "SMS"})
    void externalChannelPermanentErrorClosesChannel(COURTESY_DIGITAL_ADDRESS_TYPE_INT channel) {
        NotificationInt notification = prepareNotification(channel);
        configureSingleRetry(channel);
        Mockito.reset(externalChannelService);
        applyToExternalChannelSend(Mockito.doThrow(permanentError()));

        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        String iun = notification.getIun();
        awaitPresent(iun, channel, false);
        verifyExternalChannelSend(1);
        assertAbsent(iun, channel, true);
    }

    // ============================ ANALOG scheduling ============================

    @Test
    void analogFirstSuccessSchedulesAnalogWorkflow() {
        NotificationInt notification = prepareNotification();
        configureSingleRetry(COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP);
        Mockito.doReturn(tppResponse(true)).when(pnEmdIntegrationClientMock).sendMessage(Mockito.any(SendMessageRequestBody.class));

        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.ANALOG);

        String iun = notification.getIun();
        awaitPresent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, true);
        await().untilAsserted(() -> Assertions.assertTrue(timelineService.getTimelineElement(iun, scheduleAnalogWorkflowId(iun)).isPresent()));
    }

    @Test
    void analogAllChannelsClosedWithoutSuccessSchedulesAnalogWorkflow() {
        NotificationInt notification = prepareNotification();
        configureSingleRetry(COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP);
        Mockito.doReturn(tppResponse(false)).when(pnEmdIntegrationClientMock).sendMessage(Mockito.any(SendMessageRequestBody.class));

        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.ANALOG);

        String iun = notification.getIun();
        awaitPresent(iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, false);
        await().untilAsserted(() -> Assertions.assertTrue(timelineService.getTimelineElement(iun, scheduleAnalogWorkflowId(iun)).isPresent()));
    }

    // --- Helpers ---

    private org.mockito.stubbing.OngoingStubbing<SendMessageResponse> stubIo() {
        Mockito.reset(pnExternalRegistryClient);
        return Mockito.when(pnExternalRegistryClient.sendIOMessage(Mockito.any(SendMessageRequest.class)));
    }

    private void applyToExternalChannelSend(org.mockito.stubbing.Stubber stubber) {
        stubber.when(externalChannelService).sendCourtesyNotification(Mockito.any(NotificationInt.class),
                Mockito.any(CourtesyDigitalAddressInt.class), Mockito.anyInt(), Mockito.anyString(), Mockito.any(DeliveryModeInt.class));
    }

    private void verifyExternalChannelSend(int times) {
        Mockito.verify(externalChannelService, Mockito.times(times)).sendCourtesyNotification(Mockito.any(NotificationInt.class),
                Mockito.any(CourtesyDigitalAddressInt.class), Mockito.anyInt(), Mockito.anyString(), Mockito.any(DeliveryModeInt.class));
    }

    private NotificationInt prepareNotification(COURTESY_DIGITAL_ADDRESS_TYPE_INT... userChannels) {
        String iun = TestUtils.getRandomIun();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withPaId(PA_ID)
                .build();

        pnDeliveryClientMock.addNotification(notification);

        if (userChannels.length > 0) {
            List<CourtesyDigitalAddressInt> courtesyAddresses = new ArrayList<>();
            for (COURTESY_DIGITAL_ADDRESS_TYPE_INT channel : userChannels) {
                courtesyAddresses.add(CourtesyDigitalAddressInt.builder().address("courtesy-" + channel).type(channel).build());
            }
            addressBookMock.addCourtesyDigitalAddresses(INTERNAL_ID, PA_ID, courtesyAddresses);
        }

        return notification;
    }

    private void configureSingleRetry(COURTESY_DIGITAL_ADDRESS_TYPE_INT channel) {
        PnDeliveryPushWorkflowConfigs.CourtesyRetry.IntervalsMinutes intervalsMinutes = new PnDeliveryPushWorkflowConfigs.CourtesyRetry.IntervalsMinutes();
        switch (channel) {
            case EMAIL -> intervalsMinutes.setEmail(Collections.singletonList(0));
            case SMS -> intervalsMinutes.setSms(Collections.singletonList(0));
            case APPIO -> intervalsMinutes.setIo(Collections.singletonList(0));
            case TPP -> intervalsMinutes.setTpp(Collections.singletonList(0));
        }
        PnDeliveryPushWorkflowConfigs.CourtesyRetry courtesyRetry = new PnDeliveryPushWorkflowConfigs.CourtesyRetry();
        courtesyRetry.setIntervalsMinutes(intervalsMinutes);
        Mockito.when(cfg.getCourtesyRetry()).thenReturn(courtesyRetry);
    }

    private void awaitPresent(String iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT channel, boolean sent) {
        String elementId = sent ? sendCourtesyMessageId(channel, iun) : courtesyChannelFailedId(channel, iun);
        await().untilAsserted(() -> Assertions.assertTrue(timelineService.getTimelineElement(iun, elementId).isPresent()));
    }

    private void assertAbsent(String iun, COURTESY_DIGITAL_ADDRESS_TYPE_INT channel, boolean sent) {
        String elementId = sent ? sendCourtesyMessageId(channel, iun) : courtesyChannelFailedId(channel, iun);
        Assertions.assertTrue(timelineService.getTimelineElement(iun, elementId).isEmpty());
    }

    private static SendMessageResponse ioResult(SendMessageResponse.ResultEnum result) {
        return new SendMessageResponse().result(result);
    }

    private static it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponse tppResponse(boolean ok) {
        var response = new it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponse();
        response.setOutcome(ok
                ? it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponse.OutcomeEnum.OK
                : it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponse.OutcomeEnum.NO_CHANNELS_ENABLED);
        return response;
    }

    private static WebClientResponseException transientError() {
        return WebClientResponseException.create(500, "Internal Server Error", HttpHeaders.EMPTY, new byte[0], null);
    }

    private static WebClientResponseException permanentError() {
        return WebClientResponseException.create(400, "Bad Request", HttpHeaders.EMPTY, new byte[0], null);
    }

    private static String sendCourtesyMessageId(COURTESY_DIGITAL_ADDRESS_TYPE_INT channel, String iun) {
        return CourtesyMessageUtils.getSendCourtesyTimelineElementId(0, iun, channel, Boolean.FALSE);
    }

    private static String courtesyChannelFailedId(COURTESY_DIGITAL_ADDRESS_TYPE_INT channel, String iun) {
        return TimelineEventId.COURTESY_CHANNEL_FAILED.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(0)
                .courtesyAddressType(channel)
                .build());
    }

    private static String scheduleAnalogWorkflowId(String iun) {
        return TimelineEventId.SCHEDULE_ANALOG_WORKFLOW.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(0)
                .build());
    }
}
