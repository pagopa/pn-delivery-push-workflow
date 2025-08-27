package it.pagopa.pn.deliverypushworkflow.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.dto.address.SendInformation;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.EventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINENOTFOUND;

@Service
@Slf4j
public class ExternalChannelUtils {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;

    public ExternalChannelUtils(TimelineService timelineService,
                                TimelineUtils timelineUtils) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
    }

    public void addSendDigitalNotificationToTimeline(NotificationInt notification,
                                                     Integer recIndex,
                                                     SendInformation sendInformation,
                                                     String eventId) {
        addTimelineElement(
                timelineUtils.buildSendDigitalNotificationTimelineElement(recIndex, notification, sendInformation, eventId),
                notification
        );
    }


    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

    public String getAarKey(String iun, int recIndex) {
        
        String eventId = TimelineEventId.AAR_GENERATION.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build()
        );
        
        Optional<AarGenerationDetailsInt> aarDetailsOpt = timelineService.getTimelineElementDetails(iun, eventId, AarGenerationDetailsInt.class);

        if (aarDetailsOpt.isPresent()) {
            return aarDetailsOpt.get().getGeneratedAarUrl();
        } else {
            log.error("There isn't AAR timeline element - iun {} eventId {}", iun, eventId);
            throw new PnInternalException("There isn't AAR timeline element - iun " + iun + " eventId " + eventId, ERROR_CODE_DELIVERYPUSH_TIMELINENOTFOUND);
        }
    }
}
