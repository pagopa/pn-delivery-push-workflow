package it.pagopa.pn.deliverypushworkflow.action.it;

import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.EventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry.model.SendMessageRequest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry.model.SendMessageResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

import static org.awaitility.Awaitility.await;

/**
 * Integration tests for the courtesy long-retry flow. The whole loop is exercised end-to-end through the real
 * action pool: the dispatcher schedules a {@code SEND_COURTESY_MESSAGE_ACTION}, the pool re-dispatches it to the handler,
 * and on a retryable error the same action is rescheduled with the next retry index and the per-channel backoff interval.
 * The App IO channel is used because its send path has the fewest downstream dependencies; the outcome is driven via the
 * external registry client mock and the per-channel intervals are configured to a single immediate retry ({@code [0]}).
 */
class CourtesyMessageRetryTestIT extends CommonTestConfiguration {

    private static final String PA_ID = "paId01";
    private static final String INTERNAL_ID = "ANON_testTaxId";

    @Autowired
    PnExternalRegistryClient pnExternalRegistryClient;

    @Autowired
    CourtesyMessageUtils courtesyMessageUtils;

    @Autowired
    TimelineService timelineService;

    @Test
    void appIoRetryableErrorExhaustsRetriesAndClosesChannel() {
        // GIVEN - a single immediate retry configured and a channel that always returns a transient App IO result
        NotificationInt notification = prepareAppIoNotification();
        configureAppIoSingleRetry();
        Mockito.reset(pnExternalRegistryClient);
        Mockito.when(pnExternalRegistryClient.sendIOMessage(Mockito.any(SendMessageRequest.class)))
                .thenReturn(ioResult(SendMessageResponse.ResultEnum.ERROR_USER_STATUS));

        // WHEN - the dispatcher schedules the initial courtesy send action
        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        // THEN - after the initial attempt plus one retry the intervals are exhausted and the channel is closed
        String iun = notification.getIun();
        await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(iun, courtesyChannelFailedId(iun)).isPresent()));

        Mockito.verify(pnExternalRegistryClient, Mockito.times(2)).sendIOMessage(Mockito.any(SendMessageRequest.class));
        Assertions.assertTrue(timelineService.getTimelineElement(iun, sendCourtesyMessageId(iun)).isEmpty());
    }

    @Test
    void appIoRetryableErrorThenSuccessDeliversWithoutClosingChannel() {
        // GIVEN - the first attempt returns a transient App IO result, the retry succeeds
        NotificationInt notification = prepareAppIoNotification();
        configureAppIoSingleRetry();
        Mockito.reset(pnExternalRegistryClient);
        Mockito.when(pnExternalRegistryClient.sendIOMessage(Mockito.any(SendMessageRequest.class)))
                .thenReturn(ioResult(SendMessageResponse.ResultEnum.ERROR_USER_STATUS))
                .thenReturn(ioResult(SendMessageResponse.ResultEnum.SENT_COURTESY));

        // WHEN
        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        // THEN - the courtesy message is delivered on the retry and the channel is not closed
        String iun = notification.getIun();
        await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(iun, sendCourtesyMessageId(iun)).isPresent()));

        Mockito.verify(pnExternalRegistryClient, Mockito.times(2)).sendIOMessage(Mockito.any(SendMessageRequest.class));
        Assertions.assertTrue(timelineService.getTimelineElement(iun, courtesyChannelFailedId(iun)).isEmpty());
    }

    @Test
    void appIoPermanentErrorClosesChannelWithoutRetry() {
        // GIVEN - a single retry configured but a permanent App IO result on the first attempt
        NotificationInt notification = prepareAppIoNotification();
        configureAppIoSingleRetry();
        Mockito.reset(pnExternalRegistryClient);
        Mockito.when(pnExternalRegistryClient.sendIOMessage(Mockito.any(SendMessageRequest.class)))
                .thenReturn(ioResult(SendMessageResponse.ResultEnum.NOT_SENT_APPIO_UNAVAILABLE));

        // WHEN
        courtesyMessageUtils.scheduleCourtesyMessagesActions(notification, 0, DeliveryModeInt.DIGITAL);

        // THEN - the channel is closed immediately and no retry is scheduled
        String iun = notification.getIun();
        await().untilAsserted(() ->
                Assertions.assertTrue(timelineService.getTimelineElement(iun, courtesyChannelFailedId(iun)).isPresent()));

        Mockito.verify(pnExternalRegistryClient, Mockito.times(1)).sendIOMessage(Mockito.any(SendMessageRequest.class));
        Assertions.assertTrue(timelineService.getTimelineElement(iun, sendCourtesyMessageId(iun)).isEmpty());
    }

    private NotificationInt prepareAppIoNotification() {
        String iun = TestUtils.getRandomIun();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withPaId(PA_ID)
                .build();

        pnDeliveryClientMock.addNotification(notification);

        List<CourtesyDigitalAddressInt> courtesyAddresses = Collections.singletonList(CourtesyDigitalAddressInt.builder()
                .address("courtesy-appio")
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)
                .build());
        addressBookMock.addCourtesyDigitalAddresses(INTERNAL_ID, PA_ID, courtesyAddresses);

        return notification;
    }

    private void configureAppIoSingleRetry() {
        PnDeliveryPushWorkflowConfigs.CourtesyRetry.IntervalsMinutes intervalsMinutes = new PnDeliveryPushWorkflowConfigs.CourtesyRetry.IntervalsMinutes();
        intervalsMinutes.setIo(Collections.singletonList(0));
        PnDeliveryPushWorkflowConfigs.CourtesyRetry courtesyRetry = new PnDeliveryPushWorkflowConfigs.CourtesyRetry();
        courtesyRetry.setIntervalsMinutes(intervalsMinutes);
        Mockito.when(cfg.getCourtesyRetry()).thenReturn(courtesyRetry);
    }

    private static SendMessageResponse ioResult(SendMessageResponse.ResultEnum result) {
        return new SendMessageResponse().result(result);
    }

    private static String sendCourtesyMessageId(String iun) {
        return CourtesyMessageUtils.getSendCourtesyTimelineElementId(0, iun,
                CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, Boolean.FALSE);
    }

    private static String courtesyChannelFailedId(String iun) {
        return TimelineEventId.COURTESY_CHANNEL_FAILED.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(0)
                .courtesyAddressType(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)
                .build());
    }
}
