package it.pagopa.pn.deliverypushworkflow.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypushworkflow.action.details.SendCourtesyMessageActionDetails;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.courtesy.CourtesySendOutcome;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.io.IoSendMessageResultInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.EventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.ProbableDateAnalogWorkflowDetailsInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageRequestBody;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry.model.SendMessageResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.emdintegration.PnEmdIntegrationClient;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypushworkflow.service.*;
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
public class CourtesyMessageUtils {
    private final AddressBookService addressBookService;
    private final ExternalChannelService externalChannelService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationUtils notificationUtils;
    private final IoService iOservice;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushConfigs;
    private final PnEmdIntegrationClient pnEmdIntegrationClient;
    private final AuditLogService auditLogService;
    private final SchedulerService schedulerService;
    private final NotificationService notificationService;
    private final CourtesyRetryableErrorClassifier retryableErrorClassifier;

    /**
     * Get recipient courtesy addresses and schedule an independent send action per available channel.
     * Each channel is scheduled as its own {@link ActionType#SEND_COURTESY_MESSAGE_ACTION}, executed immediately,
     * carrying the channel, the retry index (0 = first send) and the delivery mode in its details.
     */
    public void scheduleCourtesyMessagesActions(NotificationInt notification, Integer recIndex, DeliveryModeInt deliveryMode) {
        dispatchCourtesyMessagesActions(notification, recIndex, deliveryMode);
    }

    /**
     * Dispatch the per-channel courtesy actions for the ANALOG branch and return the interim scheduling date for
     * ANALOG_WORKFLOW: the probable date (now + waiting) if at least one courtesy channel exists, otherwise now.
     * TODO WI-2.1/2.2: la decorrenza "dal primo successo" e il caso critico "tutti i canali chiusi senza successo"
     * sostituiranno questa datazione interim.
     */
    public Instant scheduleCourtesyMessagesActionsForAnalog(NotificationInt notification, Integer recIndex) {
        List<CourtesyDigitalAddressInt> scheduledChannels = dispatchCourtesyMessagesActions(notification, recIndex, DeliveryModeInt.ANALOG);
        if (scheduledChannels.isEmpty()) {
            return Instant.now();
        }
        return retrieveOrCalculateSchedulingAnalogDate(notification.getIun(), recIndex);
    }

    private List<CourtesyDigitalAddressInt> dispatchCourtesyMessagesActions(NotificationInt notification, Integer recIndex, DeliveryModeInt deliveryMode) {
        final String iun = notification.getIun();
        log.debug("Start dispatchCourtesyMessagesActions - iun={} id={} delivery mode={} ", iun, recIndex, deliveryMode);

        List<CourtesyDigitalAddressInt> listCourtesyAddresses = getCourtesyAddresses(notification, recIndex);

        for (CourtesyDigitalAddressInt courtesyAddress : listCourtesyAddresses) {
            SendCourtesyMessageActionDetails details = SendCourtesyMessageActionDetails.builder()
                    .channel(courtesyAddress.getType())
                    .retryIndex(0)
                    .deliveryMode(deliveryMode)
                    .build();
            log.info("Scheduling SEND_COURTESY_MESSAGE_ACTION channel={} retryIndex=0 deliveryMode={} - iun={} id={}", courtesyAddress.getType(), deliveryMode, iun, recIndex);
            schedulerService.scheduleEvent(iun, recIndex, Instant.now(), ActionType.SEND_COURTESY_MESSAGE_ACTION, details);
        }

        log.debug("End dispatchCourtesyMessagesActions - iun={} id={}", iun, recIndex);
        return listCourtesyAddresses;
    }

    /**
     * Execute the courtesy send for a single channel, invoked by the {@code SEND_COURTESY_MESSAGE_ACTION} handler.
     * Reuses the per-channel dispatch already present in {@link #trySendCourtesyMessage}.
     */
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
            log.warn("Courtesy address not found for channel={}, channel closed - iun={} id={}", channel, iun, recIndex);
            return;
        }

        Instant schedulingAnalogDate = retrieveOrCalculateSchedulingAnalogDate(iun, recIndex);
        CourtesySendOutcome outcome = trySendCourtesyMessage(notification, recIndex, courtesyAddress, schedulingAnalogDate, details.getDeliveryMode());

        switch (outcome) {
            case SENT -> addProbableSchedulingElementToTimeline(notification, recIndex, schedulingAnalogDate);
            case RETRYABLE_ERROR -> handleRetryableError(notification, recIndex, details);
            case PERMANENT_FAILURE -> {
                log.info("Courtesy message not sent for channel={}, permanent failure, channel closed - iun={} id={}", channel, iun, recIndex);
                addCourtesyChannelFailedToTimeline(notification, recIndex, details);
            }
        }
    }

    private void handleRetryableError(NotificationInt notification, Integer recIndex, SendCourtesyMessageActionDetails details) {
        // TODO WI-1.4: reschedule the same action with retryIndex+1 and the per-channel backoff interval;
        //  when the interval list is exhausted, route the message to the dedicated DLQ.
        //  Until the retry mechanism is in place the channel is closed (fail-safe), keeping today's behaviour.
        log.info("Retryable error on courtesy channel={} retryIndex={}, retry scheduling not yet implemented (WI-1.4), closing channel for now - iun={} id={}",
                details.getChannel(), details.getRetryIndex(), notification.getIun(), recIndex);
        addCourtesyChannelFailedToTimeline(notification, recIndex, details);
    }

    private void addCourtesyChannelFailedToTimeline(NotificationInt notification, Integer recIndex, SendCourtesyMessageActionDetails details) {
        String eventId = TimelineEventId.COURTESY_CHANNEL_FAILED.buildEventId(EventId.builder()
                .iun(notification.getIun())
                .recIndex(recIndex)
                .courtesyAddressType(details.getChannel())
                .build());
        addTimelineElement(
                timelineUtils.buildCourtesyChannelFailedTimelineElement(recIndex, notification, details.getChannel(), details.getDeliveryMode(), eventId),
                notification
        );
    }

    private CourtesyDigitalAddressInt resolveCourtesyAddress(NotificationInt notification, Integer recIndex, CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT channel) {
        return getCourtesyAddresses(notification, recIndex).stream()
                .filter(address -> channel.equals(address.getType()))
                .findFirst()
                .orElse(null);
    }

    private List<CourtesyDigitalAddressInt> getCourtesyAddresses(NotificationInt notification, Integer recIndex) {
        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);
        return addressBookService.getCourtesyAddress(recipient.getInternalId(), notification.getSender().getPaId());
    }

    private Instant retrieveOrCalculateSchedulingAnalogDate(String iun, Integer recIndex) {
        // Provo a recuperare la data dalla timeline
        String probableSchedulingElementId = getProbableSchedulingAnalogTimelineElementId(recIndex, iun);
        Instant schedulingAnalogDate = retrieveProbableSchedulingAnalogTimeline(iun, probableSchedulingElementId);
        if (schedulingAnalogDate != null) {
            log.info("Scheduling analog date found in timeline - iun={} id={} schedulingAnalogDate={}", iun, recIndex, schedulingAnalogDate);
            return schedulingAnalogDate;
        }
        // Se non esiste, la calcolo ex-novo
        Duration waitingTime = pnDeliveryPushConfigs.getTimeParams().getWaitingForReadCourtesyMessage();
        log.info("Scheduling analog date not found in timeline, calculating new one - iun={} id={} waitingTime={}", iun, recIndex, waitingTime);
        return Instant.now().plus(waitingTime);
    }

    /**
     * Tries to send the courtesy message on the given channel and classifies the outcome.
     * @return the classified {@link CourtesySendOutcome} of the attempt.
     */
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

    // --- Timeline and Utility Methods ---

    private Instant retrieveProbableSchedulingAnalogTimeline(String iun, String elementId) {
        return timelineService.getTimelineElementDetails(iun, elementId, ProbableDateAnalogWorkflowDetailsInt.class)
                .map(ProbableDateAnalogWorkflowDetailsInt::getSchedulingAnalogDate)
                .orElseGet(() -> {
                    log.info("[{}] ProbableSchedulingDateAnalogWorkflowElement is not present for elementId: {}", iun, elementId);
                    return null;
                });
    }

    public static String getSendCourtesyTimelineElementId(Integer recIndex, String iun, CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT courtesyAddressType, Boolean optin) {
        return TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(recIndex)
                .courtesyAddressType(courtesyAddressType)
                .optin(optin)
                .build()
        );
    }

    private String getProbableSchedulingAnalogTimelineElementId(Integer recIndex, String iun) {
        return TimelineEventId.PROBABLE_SCHEDULING_ANALOG_DATE.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(recIndex)
                .build()
        );
    }

    public void addSendCourtesyMessageToTimeline(NotificationInt notification, Integer recIndex, CourtesyDigitalAddressInt courtesyAddress, Instant sentDate) {
        this.addSendCourtesyMessageToTimeline(notification, recIndex, courtesyAddress, sentDate, getSendCourtesyTimelineElementId(recIndex, notification.getIun(), courtesyAddress.getType(), Boolean.FALSE),
                IoSendMessageResultInt.SENT_COURTESY);
    }

    private void addSendCourtesyMessageToTimeline(NotificationInt notification, Integer recIndex, CourtesyDigitalAddressInt courtesyAddress, Instant sentDate, String eventId,
                                                  IoSendMessageResultInt ioSendMessageResult) {
        addTimelineElement(
                timelineUtils.buildSendCourtesyMessageTimelineElement(recIndex, notification, courtesyAddress, sentDate, eventId, ioSendMessageResult),
                notification
        );
    }

    private void addProbableSchedulingElementToTimeline(NotificationInt notification, int recIndex, Instant schedulingAnalogDate) {
        String timelineElementId = getProbableSchedulingAnalogTimelineElementId(recIndex, notification.getIun());
        addTimelineElement(
                timelineUtils.buildProbableDateSchedulingAnalogTimelineElement(recIndex, notification, timelineElementId, schedulingAnalogDate),
                notification
        );
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

    // --- Gestori dei Messaggi ---

    private CourtesySendOutcome manageCourtesyMessage(NotificationInt notification, int recIndex, CourtesyDigitalAddressInt courtesyAddress, DeliveryModeInt deliveryMode) {
        log.info("Send courtesy message to externalChannel courtesyType={} - iun={} id={} ", courtesyAddress.getType(), notification.getIun(), recIndex);

        String eventId = getSendCourtesyTimelineElementId(recIndex, notification.getIun(), courtesyAddress.getType(), Boolean.FALSE);
        try {
            externalChannelService.sendCourtesyNotification(notification, courtesyAddress, recIndex, eventId, deliveryMode);
        } catch (Exception e) {
            boolean retryable = retryableErrorClassifier.isRetryableTransportError(courtesyAddress.getType(), e);
            log.warn("Error sending courtesy message on channel={} retryable={} - iun={} id={}", courtesyAddress.getType(), retryable, notification.getIun(), recIndex, e);
            return retryable ? CourtesySendOutcome.RETRYABLE_ERROR : CourtesySendOutcome.PERMANENT_FAILURE;
        }
        addSendCourtesyMessageToTimeline(notification, recIndex, courtesyAddress, Instant.now(), eventId, null);
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
            String eventId = getSendCourtesyTimelineElementId(recIndex, notification.getIun(), courtesyAddress.getType(), isOptin);

            addSendCourtesyMessageToTimeline(notification, recIndex, courtesyAddress, Instant.now(), eventId, ioSendMessageResult);
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

        String eventId = getSendCourtesyTimelineElementId(recIndex, iun, courtesyAddress.getType(), Boolean.FALSE);
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
            addSendCourtesyMessageToTimeline(notification, recIndex, courtesyAddress, Instant.now(), eventId, null);
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
