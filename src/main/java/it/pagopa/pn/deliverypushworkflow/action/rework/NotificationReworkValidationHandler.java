package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkValidationDetails;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.rework.NotificationReworkError;
import it.pagopa.pn.deliverypushworkflow.dto.rework.NotificationReworkErrorCause;
import it.pagopa.pn.deliverypushworkflow.dto.rework.NotificationReworkInfo;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.NotificationViewedCreationRequestDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.ScheduleRefinementDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.exceptions.NotificationReworkValidationException;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReworkValidationHandler {

    private static final String REC_INDEX = "RECINDEX_";
    private static final String STATUS_EFFECTIVE_DATE = "EFFECTIVE_DATE";
    private static final String STATUS_VIEWED = "VIEWED";
    private static final String STATUS_RETURNED_TO_SENDER = "RETURNED_TO_SENDER";
    private static final List<String> NOTIFICATION_STATE_FOR_TIMELINE_VALIDATION = List.of(
            STATUS_EFFECTIVE_DATE, STATUS_VIEWED, STATUS_RETURNED_TO_SENDER, "DELIVERING", "DELIVERED", "UNREACHABLE"
    );

    private final TimelineUtils timelineUtils;

    private Mono<NotificationReworkInfo> checkNotificationTimelineAndThrow(NotificationReworkInfo info) {
        String recIndex = ((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkrecIndex();

        return Flux.fromIterable(info.getTimeline())
                .filter(timelineElement -> timelineElement.getElementId().contains(REC_INDEX + recIndex))
                .switchIfEmpty(Mono.error(new NotificationReworkValidationException(NotificationReworkError.builder().cause(NotificationReworkErrorCause.INVALID_ATTEMPT_ID.getCause()).description(NotificationReworkErrorCause.INVALID_ATTEMPT_ID.getErrorDetails()).build())))
                .then(Mono.just(info))
                .flatMap(this::checkNotificationTimelineAndStatus);
    }

    private Mono<NotificationReworkInfo> checkNotificationTimelineAndStatus(NotificationReworkInfo info) {
        return Mono.just(info.getRecipientSize())
                .flatMap(recipients -> recipients == 1
                        ? checkNotificationTimelineAndStatusForMono(info)
                        : checkNotificationTimelineAndStatusForMulti(info));
    }

    private Mono<NotificationReworkInfo> checkNotificationTimelineAndStatusForMono(NotificationReworkInfo info) {
        String status = info.getNotificationStatus();
        List<TimelineElementInternal> timeline = info.getTimeline().stream().toList();

        if (STATUS_EFFECTIVE_DATE.equals(status)) {
            boolean hasSendAnalogFeedback = hasCategory(timeline, TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
            boolean hasRefinement = hasCategory(timeline, TimelineElementCategoryInt.REFINEMENT);
            boolean hasCancelled = hasCategory(timeline, TimelineElementCategoryInt.NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST);

            if (!hasSendAnalogFeedback || !hasRefinement || hasCancelled) {
                return errorMono(NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT,
                        "SEND_ANALOG_FEEDBACK : " + hasSendAnalogFeedback +
                                " REFINEMENT : " + hasRefinement +
                                " NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST : " + hasCancelled
                );
            }
        } else if (STATUS_VIEWED.equals(status)) {
            boolean hasSendAnalogFeedback = hasCategory(timeline, TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
            boolean hasRefinement = hasCategory(timeline, TimelineElementCategoryInt.REFINEMENT);
            boolean hasNotificationViewed = hasCategory(timeline, TimelineElementCategoryInt.NOTIFICATION_VIEWED);
            boolean hasCancelled = hasCategory(timeline, TimelineElementCategoryInt.NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST);

            Instant viewedDate = retrieveViewedDate(info.getAction().getIun(), getRecIndexFromAction(info.getAction()));
            Instant refinementDate = retrieveRefinementDate(info.getAction().getIun(), getRecIndexFromAction(info.getAction()));

            if (!hasRefinement && (viewedDate != null && refinementDate != null && viewedDate.isAfter(refinementDate))) {
                return errorMono(NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT,
                        "Il refinement della notifica è in corso, non è possbile procedere alla richiesta di invalidazione."
                );
            }
            if (!hasSendAnalogFeedback || !hasRefinement || !hasNotificationViewed || hasCancelled) {
                return errorMono(NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT,
                        "SEND_ANALOG_FEEDBACK : " + hasSendAnalogFeedback +
                                " REFINEMENT : " + hasRefinement +
                                " NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST : " + hasCancelled
                );
            }
        } else if (STATUS_RETURNED_TO_SENDER.equals(status)) {
            boolean hasSendAnalogFeedback = hasCategory(timeline, TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
            boolean hasAnalogWorkflow = hasCategory(timeline, TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED);
            boolean hasCancelled = hasCategory(timeline, TimelineElementCategoryInt.NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST);

            if (!hasSendAnalogFeedback || !hasAnalogWorkflow || hasCancelled) {
                return errorMono(NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT,
                        "SEND_ANALOG_FEEDBACK : " + hasSendAnalogFeedback +
                                " ANALOG_WORKFLOW_RECIPIENT_DECEASED : " + hasAnalogWorkflow +
                                " NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST : " + hasCancelled
                );
            }
        }
        return Mono.just(info);
    }

    private Mono<NotificationReworkInfo> checkNotificationTimelineAndStatusForMulti(NotificationReworkInfo info) {
        String status = info.getNotificationStatus();
        List<TimelineElementInternal> timeline = info.getTimeline().stream().toList();

        if (NOTIFICATION_STATE_FOR_TIMELINE_VALIDATION.contains(status)) {
            boolean hasSendAnalogFeedback = hasCategory(timeline, TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
            boolean hasRefinement = hasCategory(timeline, TimelineElementCategoryInt.REFINEMENT);
            boolean hasNotificationViewed = hasCategory(timeline, TimelineElementCategoryInt.NOTIFICATION_VIEWED);
            boolean hasAnalogWorkflowRecipientDeceased = hasCategory(timeline, TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED);
            boolean hasCancelled = hasCategory(timeline, TimelineElementCategoryInt.NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST);

            Instant viewedDate = retrieveViewedDate(info.getAction().getIun(), getRecIndexFromAction(info.getAction()));
            Instant refinementDate = retrieveRefinementDate(info.getAction().getIun(), getRecIndexFromAction(info.getAction()));

            if (hasNotificationViewed && !hasRefinement && (viewedDate != null && refinementDate != null && viewedDate.isAfter(refinementDate))) {
                return errorMono(NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT,
                        "Il refinement della notifica è in corso, non è possbile procedere alla richiesta di invalidazione."
                );
            }
            if (!hasSendAnalogFeedback || !(hasRefinement || hasAnalogWorkflowRecipientDeceased) || hasCancelled) {
                return errorMono(NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT,
                        "SEND_ANALOG_FEEDBACK : " + hasSendAnalogFeedback +
                                " REFINEMENT : " + hasRefinement +
                                " NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST : " + hasCancelled
                );
            }
        }
        return Mono.just(info);
    }

    private int getRecIndexFromAction(Action action) {
        String recIndex = ((NotificationReworkValidationDetails) action.getDetails()).getReworkrecIndex();
        return Integer.parseInt(recIndex.substring(REC_INDEX.length()));
    }

    @Nullable
    private Instant retrieveViewedDate(String iun, Integer recIndex) {
        return timelineUtils.getNotificationViewCreationRequest(iun, recIndex)
                .map(elem -> {
                    if (elem.getDetails() instanceof NotificationViewedCreationRequestDetailsInt details) {
                        return details.getEventTimestamp();
                    }
                    return null;
                }).orElse(null);
    }

    @Nullable
    private Instant retrieveRefinementDate(String iun, Integer recIndex) {
        Instant refinementDate = timelineUtils.getScheduleRefinement(iun, recIndex)
                .map(elem -> {
                    if (elem.getDetails() instanceof ScheduleRefinementDetailsInt details) {
                        return details.getSchedulingDate();
                    }
                    return null;
                }).orElse(null);

        if (refinementDate == null)
            log.error("Schedule refinement not found iun={} recIndex={}", iun, recIndex);
        else
            log.info("Schedule refinement date iun={} recIndex={} refinementDate={}", iun, recIndex, refinementDate);

        return refinementDate;
    }

    private boolean hasCategory(List<TimelineElementInternal> timeline, TimelineElementCategoryInt category) {
        return timeline.stream().anyMatch(e -> e.getCategory() == category);
    }

    private Mono<NotificationReworkInfo> errorMono(NotificationReworkErrorCause cause, String details) {
        String errorDescription = String.format(cause.getErrorDetails(), details);
        return Mono.error(new NotificationReworkValidationException(
                NotificationReworkError.builder()
                        .cause(cause.getCause())
                        .description(errorDescription)
                        .build()
        ));
    }
}