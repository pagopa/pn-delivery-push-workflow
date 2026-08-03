package it.pagopa.pn.deliverypushworkflow.action.courtesymessage;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypushworkflow.action.details.SendCourtesyMessageActionDetails;
import it.pagopa.pn.deliverypushworkflow.action.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.courtesy.CourtesySendOutcome;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.io.IoSendMessageResultInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.EventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.CourtesyChannelFailedDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.CourtesyChannelFailureReasonInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.ProbableDateAnalogWorkflowDetailsInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageRequestBody;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry.model.SendMessageResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.emdintegration.PnEmdIntegrationClient;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypushworkflow.service.*;
import it.pagopa.pn.deliverypushworkflow.action.utils.CourtesyRetryableErrorClassifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ERRORCOURTESY;
import static it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry.model.SendMessageResponse.ResultEnum.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class SendCourtesyMessageHandler {
    private final CourtesyMessageUtils courtesyMessageUtils;
    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final ExternalChannelService externalChannelService;
    private final IoService iOservice;
    private final PnEmdIntegrationClient pnEmdIntegrationClient;
    private final AuditLogService auditLogService;
    private final SchedulerService schedulerService;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushConfigs;
    private final CourtesyRetryableErrorClassifier retryableErrorClassifier;

    /** Entry point for the {@code SEND_COURTESY_MESSAGE_ACTION}: send the courtesy on a single channel and handle the outcome. */
    public void handleSendCourtesyMessageAction(String iun, Integer recIndex, SendCourtesyMessageActionDetails details) {
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT channel = details.getChannel();
        log.info("handleSendCourtesyMessageAction channel={} retryIndex={} deliveryMode={} - iun={} id={}", channel, details.getRetryIndex(), details.getDeliveryMode(), iun, recIndex);

        if (timelineUtils.checkIsNotificationCancellationRequested(iun)) {
            log.warn("Notification cancellation requested, skipping courtesy send for channel={} - iun={} id={}", channel, iun, recIndex);
            return;
        }

        CourtesyDigitalAddressInt courtesyAddress = resolveCourtesyAddress(notification, recIndex, channel);
        if (courtesyAddress == null) {
            log.warn("Courtesy address not found for channel={}, closing channel - iun={} id={}", channel, iun, recIndex);
            closeCourtesyChannelWithoutSuccess(notification, recIndex, details, CourtesyChannelFailureReasonInt.EXPECTED_FAILURE);
            return;
        }

        Instant schedulingAnalogDate = retrieveOrCalculateSchedulingAnalogDate(iun, recIndex);
        CourtesySendOutcome outcome = trySendCourtesyMessage(notification, recIndex, courtesyAddress, schedulingAnalogDate, details.getDeliveryMode());

        switch (outcome) {
            case SENT -> {
                log.info("Courtesy message sent successfully for channel={} retryIndex={} - iun={} id={}", channel, details.getRetryIndex(), iun, recIndex);
                if (details.getDeliveryMode() == DeliveryModeInt.ANALOG) {
                    courtesyMessageUtils.scheduleAnalogWorkflow(notification, recIndex, schedulingAnalogDate);
                }
            }
            case RETRYABLE_ERROR -> {
                log.info("Retryable error on courtesy channel={} retryIndex={} - iun={} id={}", channel, details.getRetryIndex(), iun, recIndex);
                handleRetryableError(notification, recIndex, details);
            }
            case PERMANENT_FAILURE -> {
                log.info("Courtesy message not sent for channel={}, permanent failure, channel closed - iun={} id={}", channel, iun, recIndex);
                closeCourtesyChannelWithoutSuccess(notification, recIndex, details, CourtesyChannelFailureReasonInt.EXPECTED_FAILURE);
            }
        }
    }

    /**
     * Reschedule the same action with the next backoff interval; the incremented retryIndex is carried in the details
     * so the resulting {@code actionId} stays unique and is not deduplicated by pn-action-manager.
     */
    private void handleRetryableError(NotificationInt notification, Integer recIndex, SendCourtesyMessageActionDetails details) {
        final String iun = notification.getIun();
        CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT channel = details.getChannel();
        List<Integer> intervals = resolveRetryIntervalsMinutes(channel);
        int currentRetryIndex = details.getRetryIndex();

        if (currentRetryIndex >= intervals.size()) {
            log.info("Courtesy retry intervals exhausted for channel={} retryIndex={}, channel closed - iun={} id={}",
                    channel, currentRetryIndex, iun, recIndex);
            closeCourtesyChannelWithoutSuccess(notification, recIndex, details, CourtesyChannelFailureReasonInt.RETRIES_EXHAUSTED);
            return;
        }

        int waitMinutes = intervals.get(currentRetryIndex);
        int nextRetryIndex = currentRetryIndex + 1;
        Instant schedulingDate = Instant.now().plus(Duration.ofMinutes(waitMinutes));
        SendCourtesyMessageActionDetails nextDetails = SendCourtesyMessageActionDetails.builder()
                .channel(channel)
                .retryIndex(nextRetryIndex)
                .deliveryMode(details.getDeliveryMode())
                .plannedChannels(details.getPlannedChannels())
                .build();
        log.info("Rescheduling SEND_COURTESY_MESSAGE_ACTION channel={} nextRetryIndex={} waitMinutes={} schedulingDate={} - iun={} id={}",
                channel, nextRetryIndex, waitMinutes, schedulingDate, iun, recIndex);
        schedulerService.scheduleEvent(iun, recIndex, schedulingDate, ActionType.SEND_COURTESY_MESSAGE_ACTION, nextDetails);
    }

    /** Per-channel backoff intervals (minutes): size = number of retries, each value = wait before that retry; empty = no retry. */
    private List<Integer> resolveRetryIntervalsMinutes(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT channel) {
        PnDeliveryPushWorkflowConfigs.CourtesyRetry courtesyRetry = pnDeliveryPushConfigs.getCourtesyRetry();
        if (courtesyRetry == null || courtesyRetry.getIntervalsMinutes() == null) {
            return List.of();
        }
        PnDeliveryPushWorkflowConfigs.CourtesyRetry.IntervalsMinutes intervalsMinutes = courtesyRetry.getIntervalsMinutes();
        List<Integer> channelIntervals = switch (channel) {
            case EMAIL -> intervalsMinutes.getEmail();
            case SMS -> intervalsMinutes.getSms();
            case APPIO -> intervalsMinutes.getIo();
            case TPP -> intervalsMinutes.getTpp();
        };
        return channelIntervals != null ? channelIntervals : List.of();
    }

    /** Record the channel failure on the timeline and, on the ANALOG branch, evaluate whether to start the analog workflow. */
    private void closeCourtesyChannelWithoutSuccess(NotificationInt notification, Integer recIndex, SendCourtesyMessageActionDetails details, CourtesyChannelFailureReasonInt failureReason) {
        addCourtesyChannelFailedToTimeline(notification, recIndex, details, failureReason);
        if (details.getDeliveryMode() == DeliveryModeInt.ANALOG) {
            scheduleAnalogWorkflowIfAllChannelsClosedWithoutSuccess(notification, recIndex, details.getPlannedChannels());
        }
    }

    private void addCourtesyChannelFailedToTimeline(NotificationInt notification, Integer recIndex, SendCourtesyMessageActionDetails details, CourtesyChannelFailureReasonInt failureReason) {
        String eventId = courtesyChannelFailedEventId(notification.getIun(), recIndex, details.getChannel());
        timelineService.addTimelineElement(
                timelineUtils.buildCourtesyChannelFailedTimelineElement(recIndex, notification, details.getChannel(), details.getDeliveryMode(), failureReason, eventId),
                notification
        );
    }

    /**
     * ANALOG coordination via strongly consistent reads: if every channel is now closed with no delivery, schedule
     * ANALOG_WORKFLOW immediately; a delivered channel keeps the +waiting scheduling and wins. Idempotent through the
     * deterministic ANALOG_WORKFLOW actionId, so concurrent closures start it once. The expected channels are the set
     * frozen at dispatch (carried in the action), so adding or removing a courtesy address during the retry window
     * cannot alter the coordination and block the notification.
     */
    private void scheduleAnalogWorkflowIfAllChannelsClosedWithoutSuccess(NotificationInt notification, Integer recIndex,
                                                                         List<CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT> plannedChannels) {
        final String iun = notification.getIun();
        boolean anySuccess = false;
        for (CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT channel : plannedChannels) {
            if (isCourtesyChannelDelivered(iun, recIndex, channel)) {
                anySuccess = true;
            } else if (!isCourtesyChannelFailedForAnalog(iun, recIndex, channel)) {
                log.info("Courtesy channel={} still open, analog workflow not scheduled yet - iun={} id={}", channel, iun, recIndex);
                return;
            }
        }

        if (anySuccess) {
            log.info("At least one courtesy channel delivered, analog workflow already scheduled from the first success - iun={} id={}", iun, recIndex);
            return;
        }

        log.info("All courtesy channels closed without success, scheduling analog workflow now - iun={} id={}", iun, recIndex);
        courtesyMessageUtils.scheduleAnalogWorkflow(notification, recIndex, Instant.now());
    }

    private boolean isCourtesyChannelDelivered(String iun, Integer recIndex, CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT channel) {
        if (timelineService.getTimelineElementStrongly(iun, CourtesyMessageUtils.getSendCourtesyTimelineElementId(recIndex, iun, channel, Boolean.FALSE)).isPresent()) {
            return true;
        }
        // App IO records opt-in deliveries under a dedicated elementId
        return channel == CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO
                && timelineService.getTimelineElementStrongly(iun, CourtesyMessageUtils.getSendCourtesyTimelineElementId(recIndex, iun, channel, Boolean.TRUE)).isPresent();
    }

    private boolean isCourtesyChannelFailedForAnalog(String iun, Integer recIndex, CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT channel) {
        return timelineService.getTimelineElementStrongly(iun, courtesyChannelFailedEventId(iun, recIndex, channel))
                .map(TimelineElementInternal::getDetails)
                .filter(CourtesyChannelFailedDetailsInt.class::isInstance)
                .map(CourtesyChannelFailedDetailsInt.class::cast)
                .filter(failed -> failed.getDeliveryMode() == DeliveryModeInt.ANALOG)
                .isPresent();
    }

    private static String courtesyChannelFailedEventId(String iun, Integer recIndex, CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT channel) {
        return TimelineEventId.COURTESY_CHANNEL_FAILED.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(recIndex)
                .courtesyAddressType(channel)
                .build());
    }

    private CourtesyDigitalAddressInt resolveCourtesyAddress(NotificationInt notification, Integer recIndex, CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT channel) {
        return courtesyMessageUtils.getCourtesyAddresses(notification, recIndex).stream()
                .filter(address -> channel.equals(address.getType()))
                .findFirst()
                .orElse(null);
    }

    private Instant retrieveOrCalculateSchedulingAnalogDate(String iun, Integer recIndex) {
        String probableSchedulingElementId = courtesyMessageUtils.getProbableSchedulingAnalogTimelineElementId(recIndex, iun);
        Instant schedulingAnalogDate = retrieveProbableSchedulingAnalogTimeline(iun, probableSchedulingElementId);
        if (schedulingAnalogDate != null) {
            log.info("Scheduling analog date found in timeline - iun={} id={} schedulingAnalogDate={}", iun, recIndex, schedulingAnalogDate);
            return schedulingAnalogDate;
        }
        Duration waitingTime = pnDeliveryPushConfigs.getTimeParams().getWaitingForReadCourtesyMessage();
        log.info("Scheduling analog date not found in timeline, calculating new one - iun={} id={} waitingTime={}", iun, recIndex, waitingTime);
        return Instant.now().plus(waitingTime);
    }

    private Instant retrieveProbableSchedulingAnalogTimeline(String iun, String elementId) {
        return timelineService.getTimelineElementDetails(iun, elementId, ProbableDateAnalogWorkflowDetailsInt.class)
                .map(ProbableDateAnalogWorkflowDetailsInt::getSchedulingAnalogDate)
                .orElseGet(() -> {
                    log.info("[{}] ProbableSchedulingDateAnalogWorkflowElement is not present for elementId: {}", iun, elementId);
                    return null;
                });
    }

    private CourtesySendOutcome trySendCourtesyMessage(NotificationInt notification,
                                                       Integer recIndex,
                                                       CourtesyDigitalAddressInt courtesyAddress,
                                                       Instant schedulingAnalogDate,
                                                       DeliveryModeInt deliveryMode) {

        log.debug("Send courtesy message attempt for address type {} - iun={} id={}", courtesyAddress.getType(), notification.getIun(), recIndex);

        if (timelineUtils.checkIsNotificationCancellationRequested(notification.getIun())) {
            log.warn("{} courtesy blocked for cancelled notification iun={}", courtesyAddress.getType(), notification.getIun());
            return CourtesySendOutcome.PERMANENT_FAILURE;
        }

        CourtesySendOutcome outcome;
        switch (courtesyAddress.getType()) {
            case EMAIL, SMS -> outcome = manageCourtesyMessage(notification, recIndex, courtesyAddress, deliveryMode);
            case APPIO -> outcome = manageIOMessage(notification, recIndex, courtesyAddress, schedulingAnalogDate, deliveryMode);
            case TPP -> outcome = manageTPPMessage(notification, recIndex, courtesyAddress, deliveryMode, schedulingAnalogDate);
            default -> {
                handleCourtesyTypeError(notification, recIndex, courtesyAddress);
                outcome = CourtesySendOutcome.PERMANENT_FAILURE;
            }
        }

        return outcome;
    }

    private void handleCourtesyTypeError(NotificationInt notification, Integer recIndex, CourtesyDigitalAddressInt courtesyAddress) {
        log.error("Is not possibile to send courtesy message, courtesyAddressType={} is not defined - iun={} id={}",
                courtesyAddress.getType(), notification.getIun(), recIndex);
        throw new PnInternalException("Is not possibile to send courtesy message, courtesyAddressType=" + courtesyAddress.getType() +
                " is not defined - iun=" + notification.getIun() + " id=" + recIndex, ERROR_CODE_DELIVERYPUSH_ERRORCOURTESY);
    }

    private CourtesySendOutcome manageCourtesyMessage(NotificationInt notification, int recIndex, CourtesyDigitalAddressInt courtesyAddress, DeliveryModeInt deliveryMode) {
        log.info("Send courtesy message to externalChannel courtesyType={} - iun={} id={} ", courtesyAddress.getType(), notification.getIun(), recIndex);

        String eventId = CourtesyMessageUtils.getSendCourtesyTimelineElementId(recIndex, notification.getIun(), courtesyAddress.getType(), Boolean.FALSE);
        try {
            externalChannelService.sendCourtesyNotification(notification, courtesyAddress, recIndex, eventId, deliveryMode);
        } catch (Exception e) {
            boolean retryable = retryableErrorClassifier.isRetryableTransportError(courtesyAddress.getType(), e);
            log.warn("Error sending courtesy message on channel={} retryable={} - iun={} id={}", courtesyAddress.getType(), retryable, notification.getIun(), recIndex, e);
            return retryable ? CourtesySendOutcome.RETRYABLE_ERROR : CourtesySendOutcome.PERMANENT_FAILURE;
        }
        courtesyMessageUtils.addSendCourtesyMessageToTimeline(notification, recIndex, courtesyAddress, Instant.now(), eventId, null);
        return CourtesySendOutcome.SENT;
    }

    private CourtesySendOutcome manageIOMessage(NotificationInt notification, int recIndex, CourtesyDigitalAddressInt courtesyAddress, Instant schedulingAnalogDate, DeliveryModeInt deliveryMode) {
        // App IO may not actually send the message: besides transport exceptions (classified below),
        // there are cases where the message is not sent because the user has not enabled App IO;
        // in those cases no timeline event is saved. The same applies to the opt-in flow.
        log.info("Send courtesy message to App IO - iun={} id={} ", notification.getIun(), recIndex);

        SendMessageResponse.ResultEnum result;
        try {
            result = iOservice.sendIOMessage(notification, recIndex, schedulingAnalogDate, deliveryMode);
        } catch (Exception e) {
            boolean retryable = retryableErrorClassifier.isRetryableTransportError(courtesyAddress.getType(), e);
            log.warn("Error sending courtesy message to App IO retryable={} - iun={} id={}", retryable, notification.getIun(), recIndex, e);
            return retryable ? CourtesySendOutcome.RETRYABLE_ERROR : CourtesySendOutcome.PERMANENT_FAILURE;
        }

        if (SENT_COURTESY.equals(result) || SENT_OPTIN.equals(result) || NOT_SENT_OPTIN_ALREADY_SENT.equals(result)) {
            IoSendMessageResultInt ioSendMessageResult = IoSendMessageResultInt.valueOf(result.getValue());
            boolean isOptin = SENT_OPTIN.equals(result) || NOT_SENT_OPTIN_ALREADY_SENT.equals(result);
            String eventId = CourtesyMessageUtils.getSendCourtesyTimelineElementId(recIndex, notification.getIun(), courtesyAddress.getType(), isOptin);

            courtesyMessageUtils.addSendCourtesyMessageToTimeline(notification, recIndex, courtesyAddress, Instant.now(), eventId, ioSendMessageResult);
            return CourtesySendOutcome.SENT;
        }

        if (retryableErrorClassifier.isRetryableIoResult(result)) {
            log.info("App IO returned a transient error result={} - iun={} id={}", result, notification.getIun(), recIndex);
            return CourtesySendOutcome.RETRYABLE_ERROR;
        }

        log.info("App IO did not send the courtesy message, permanent result={} - iun={} id={}", result, notification.getIun(), recIndex);
        return CourtesySendOutcome.PERMANENT_FAILURE;
    }

    private CourtesySendOutcome manageTPPMessage(NotificationInt notification, Integer recIndex, CourtesyDigitalAddressInt courtesyAddress, DeliveryModeInt deliveryMode, Instant schedulingAnalogDate) {
        final String iun = notification.getIun();
        log.info("manageTPPMessage - iun={} id={} ", iun, recIndex);

        String eventId = CourtesyMessageUtils.getSendCourtesyTimelineElementId(recIndex, iun, courtesyAddress.getType(), Boolean.FALSE);
        SendMessageRequestBody request = buildSendMessageRequest(notification, recIndex, deliveryMode, schedulingAnalogDate);
        PnAuditLogEvent logEvent = buildAuditLogEvent(iun, recIndex, eventId);

        it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponse response;
        try {
            response = pnEmdIntegrationClient.sendMessage(request);
        } catch (Exception e) {
            boolean retryable = retryableErrorClassifier.isRetryableTransportError(courtesyAddress.getType(), e);
            logEvent.generateFailure("Error sending courtesy message via TPP channel retryable={} with recIndex={} and iun={}", retryable, recIndex, iun, e).log();
            return retryable ? CourtesySendOutcome.RETRYABLE_ERROR : CourtesySendOutcome.PERMANENT_FAILURE;
        }

        if (response.getOutcome() == it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponse.OutcomeEnum.OK) {
            courtesyMessageUtils.addSendCourtesyMessageToTimeline(notification, recIndex, courtesyAddress, Instant.now(), eventId, null);
            logEvent.generateSuccess("successful sent courtesy message via TPP channel with recIndex ={} and iun ={}", recIndex, iun).log();
            return CourtesySendOutcome.SENT;
        } else {
            logEvent.generateSuccess("TPP channel not enabled for recipient with recIndex={} and iun={}", recIndex, iun).log();
            return CourtesySendOutcome.PERMANENT_FAILURE;
        }
    }

    private SendMessageRequestBody buildSendMessageRequest(NotificationInt notification, Integer recIndex, DeliveryModeInt deliveryMode, Instant schedulingAnalogDate) {
        return new SendMessageRequestBody()
                .recipientId(notification.getRecipients().get(recIndex).getTaxId())
                .internalRecipientId(notification.getRecipients().get(recIndex).getInternalId())
                .originId(notification.getIun())
                .senderDescription(notification.getSender().getPaDenomination())
                .associatedPayment(hasRecipientPagoPaPayment(notification, recIndex))
                .deliveryMode(SendMessageRequestBody.DeliveryModeEnum.fromValue(deliveryMode.getValue()))
                .schedulingAnalogDate(deliveryMode == DeliveryModeInt.ANALOG ? schedulingAnalogDate : null);
    }

    private PnAuditLogEvent buildAuditLogEvent(String iun, int recIndex, String eventId) {
        return auditLogService.buildAuditLogEvent(iun, recIndex, PnAuditLogEventType.AUD_DA_SEND_TPP, "sendTppMessage eventId={}", eventId);
    }

    private boolean hasRecipientPagoPaPayment(NotificationInt notification, Integer recIndex) {
        if(CollectionUtils.isEmpty(notification.getRecipients().get(recIndex).getPayments())) {
            return false;
        }
        return notification.getRecipients().get(recIndex).getPayments().stream()
                .anyMatch(paymentInfo -> paymentInfo.getPagoPA() != null);
    }

}
