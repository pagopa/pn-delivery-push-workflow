package it.pagopa.pn.deliverypushworkflow.action.utils;

import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.NotificationTimelineReworkedDetailsInt;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkConstant.*;
import static it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt.NOTIFICATION_TIMELINE_REWORKED;
import static it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt.SEND_ANALOG_DOMICILE;

public class NotificationReworkUtils {
    private NotificationReworkUtils() {}

    public static boolean checkNotificationExpectedFinalStatusCodeAndThrow(
            String expectedAttempt,
            String expectedStatus,
            String reworkIndex,
            Set<TimelineElementInternal> timeline) {
        Set<TimelineElementInternal> filteredOnRecIndexTimelineElments = timeline.stream()
                .filter(timelineElement -> timelineElement.getElementId().contains(reworkIndex))
                .collect(Collectors.toSet());

        boolean hasAttempt0 = filteredOnRecIndexTimelineElments.stream().anyMatch(timelineElement -> timelineElement.getElementId().contains(ATTEMPT_0));
        boolean hasAttempt1 = filteredOnRecIndexTimelineElments.stream().anyMatch(timelineElement -> timelineElement.getCategory().equals(SEND_ANALOG_DOMICILE) &&
                timelineElement.getElementId().contains(ATTEMPT_1));

        if(expectedAttempt.equalsIgnoreCase(ATTEMPT_0)){
            List<NotificationTimelineReworkedDetailsInt> notificationTimelineReworkedDetailsIntList =  filteredOnRecIndexTimelineElments.stream()
                    .filter(timelineElementInternal -> timelineElementInternal.getCategory().equals(NOTIFICATION_TIMELINE_REWORKED))
                    .map(timelineElementInternal -> (NotificationTimelineReworkedDetailsInt) timelineElementInternal.getDetails())
                    .toList();

            boolean containsInvalidatedAttempt1 = notificationTimelineReworkedDetailsIntList.stream()
                    .flatMap(detail -> detail.getInvalidatedTimelineAndStatusHistory().stream())
                    .flatMap(historyElement -> historyElement.getRelatedTimelineElementIds().stream())
                    .anyMatch(timelineElementId -> timelineElementId.contains(TimelineEventId.SEND_ANALOG_DOMICILE.getValue()) && timelineElementId.contains(ATTEMPT_1));

            hasAttempt1 = hasAttempt1 || containsInvalidatedAttempt1;
        }

        return !hasAttempt0 || !hasAttempt1 || !ATTEMPT_0.equals(expectedAttempt) || !KO.equals(expectedStatus);
    }
}

