package it.pagopa.pn.deliverypushworkflow.action.utils;

import it.pagopa.pn.deliverypushworkflow.action.details.SendCourtesyMessageActionDetails;
import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.io.IoSendMessageResultInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.EventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypushworkflow.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CourtesyMessageUtils {
    private final AddressBookService addressBookService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationUtils notificationUtils;
    private final SchedulerService schedulerService;

    /**
     * Schedule an independent {@link ActionType#SEND_COURTESY_MESSAGE_ACTION} per available courtesy channel, executed
     * immediately. On the ANALOG branch with no channel available the analog workflow is scheduled immediately, since
     * no delivery can ever succeed.
     */
    public void scheduleCourtesyMessagesActions(NotificationInt notification, Integer recIndex, DeliveryModeInt deliveryMode) {
        List<CourtesyDigitalAddressInt> scheduledChannels = dispatchCourtesyMessagesActions(notification, recIndex, deliveryMode);
        if (deliveryMode == DeliveryModeInt.ANALOG && scheduledChannels.isEmpty()) {
            log.info("No courtesy channel available, scheduling analog workflow immediately - iun={} id={}", notification.getIun(), recIndex);
            scheduleAnalogWorkflow(notification, recIndex, Instant.now());
        }
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

    public List<CourtesyDigitalAddressInt> getCourtesyAddresses(NotificationInt notification, Integer recIndex) {
        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);
        return addressBookService.getCourtesyAddress(recipient.getInternalId(), notification.getSender().getPaId());
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

    public String getProbableSchedulingAnalogTimelineElementId(Integer recIndex, String iun) {
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

    public void addSendCourtesyMessageToTimeline(NotificationInt notification, Integer recIndex, CourtesyDigitalAddressInt courtesyAddress, Instant sentDate, String eventId,
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

    public void scheduleAnalogWorkflow(NotificationInt notification, Integer recIndex, Instant schedulingAnalogDate) {
        addProbableSchedulingElementToTimeline(notification, recIndex, schedulingAnalogDate);
        addScheduleAnalogWorkflowToTimeline(notification, recIndex, schedulingAnalogDate);
        schedulerService.scheduleEvent(notification.getIun(), recIndex, schedulingAnalogDate, ActionType.ANALOG_WORKFLOW);
    }

    private void addScheduleAnalogWorkflowToTimeline(NotificationInt notification, Integer recIndex, Instant schedulingAnalogDate) {
        addTimelineElement(
                timelineUtils.buildScheduleAnalogWorkflowTimeline(notification, recIndex, schedulingAnalogDate),
                notification
        );
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

}
