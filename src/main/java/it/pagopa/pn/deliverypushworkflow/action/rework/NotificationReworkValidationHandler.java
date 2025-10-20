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

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReworkValidationHandler {

    private final String REC_INDEX = "RECINDEX_";

    private final TimelineUtils timelineUtils;

    private Mono<NotificationReworkInfo> checkNotificationTimelineAndThrow(NotificationReworkInfo info) {
        String recIndex = ((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkrecIndex();

        return Flux.fromIterable(info.getTimeline())
                .filter(timelineElement -> timelineElement.getElementId().contains(REC_INDEX+recIndex))
                .switchIfEmpty(Mono.error(new NotificationReworkValidationException(NotificationReworkError.builder().cause(NotificationReworkErrorCause.INVALID_ATTEMPT_ID.getCause()).description(NotificationReworkErrorCause.INVALID_ATTEMPT_ID.getErrorDetails()).build())))
                .then(Mono.just(info))
                .flatMap(this::checkNotificationTimelineAndStatus);
    }

    private Mono<NotificationReworkInfo> checkNotificationTimelineAndStatus(NotificationReworkInfo info) {
        return Mono.just(info.getRecipientSize())
                .flatMap(recipients -> {
                    if (recipients == 1) {
                        return checkNotificationTimelineAndStatusForMono(info);
                    } else {
                        return checkNotificationTimelineAndStatusForMulti(info);
                    }
                });
    }

    private Mono<NotificationReworkInfo> checkNotificationTimelineAndStatusForMono(NotificationReworkInfo info) {
        return Mono.just(info.getNotificationStatus())
                .flatMap(status -> {
                    if ("EFFECTIVE_DATE".equals(status)) {
                        boolean hasSendAnalogFeedback = info.getTimeline().stream()
                                .anyMatch(e -> e.getCategory() == TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
                        boolean hasRefinement = info.getTimeline().stream()
                                .anyMatch(e -> e.getCategory() == TimelineElementCategoryInt.REFINEMENT);
                        boolean hasNotificationCancelledDocumentCreationRequest = info.getTimeline().stream()
                                .anyMatch(e -> e.getCategory() == TimelineElementCategoryInt.NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST);


                        if (!hasSendAnalogFeedback || !hasRefinement || hasNotificationCancelledDocumentCreationRequest) {
                            String errorDescription = String.format(
                                    NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT.getErrorDetails(),
                                    "SEND_ANALOG_FEEDBACK : " + hasSendAnalogFeedback + " REFINEMENT : " + hasRefinement + " NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST : " + hasNotificationCancelledDocumentCreationRequest
                            );
                            return Mono.error(new NotificationReworkValidationException(
                                    NotificationReworkError.builder()
                                            .cause(NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT.getCause())
                                            .description(errorDescription)
                                            .build()
                            ));
                        }
                        return Mono.just(info);
                    } else if ("VIEWED".equals(status)) {
                        boolean hasSendAnalogFeedback = info.getTimeline().stream()
                                .anyMatch(e -> e.getCategory() == TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
                        boolean hasRefinement = info.getTimeline().stream()
                                .anyMatch(e -> e.getCategory() == TimelineElementCategoryInt.REFINEMENT);
                        boolean hasNotificationViewed = info.getTimeline().stream()
                                .anyMatch(e -> e.getCategory() == TimelineElementCategoryInt.NOTIFICATION_VIEWED);
                        boolean hasNotificationCancelledDocumentCreationRequest = info.getTimeline().stream()
                                .anyMatch(e -> e.getCategory() == TimelineElementCategoryInt.NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST);

                        Instant viewedDate = retrieveViewedDate(info.getAction().getIun(), getRecIndexFromAction(info.getAction()));

                        Instant refinementDate = retrieveRefinementDate(info.getAction().getIun(), getRecIndexFromAction(info.getAction()));

                        if(!hasRefinement && (viewedDate != null && refinementDate != null && viewedDate.isAfter(refinementDate))) {
                            String errorDescription = String.format(
                                    NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT.getErrorDetails(),
                                    "Il refinement della notifica è in corso, non è possbile procedere alla richiesta di invalidazione."
                            );
                            return Mono.error(new NotificationReworkValidationException(
                                    NotificationReworkError.builder()
                                            .cause(NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT.getCause())
                                            .description(errorDescription)
                                            .build()
                            ));
                        }
                        if (!hasSendAnalogFeedback || !hasRefinement || !hasNotificationViewed || hasNotificationCancelledDocumentCreationRequest) {
                            String errorDescription = String.format(
                                    NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT.getErrorDetails(),
                                    "SEND_ANALOG_FEEDBACK : " + hasSendAnalogFeedback + " REFINEMENT : " + hasRefinement + " NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST : " + hasNotificationCancelledDocumentCreationRequest
                            );
                            return Mono.error(new NotificationReworkValidationException(
                                    NotificationReworkError.builder()
                                            .cause(NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT.getCause())
                                            .description(errorDescription)
                                            .build()
                            ));
                        }
                        return Mono.just(info);
                    } else if ("RETURNED_TO_SENDER".equals(status)) {
                        boolean hasSendAnalogFeedback = info.getTimeline().stream()
                                .anyMatch(e -> e.getCategory() == TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
                        boolean hasAnalogWorkflow = info.getTimeline().stream()
                                .anyMatch(e -> e.getCategory() == TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED);
                        boolean hasNotificationCancelledDocumentCreationRequest = info.getTimeline().stream()
                                .anyMatch(e -> e.getCategory() == TimelineElementCategoryInt.NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST);

                        if (!hasSendAnalogFeedback || !hasAnalogWorkflow || hasNotificationCancelledDocumentCreationRequest) {
                            String errorDescription = String.format(
                                    NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT.getErrorDetails(),
                                    "SEND_ANALOG_FEEDBACK : " + hasSendAnalogFeedback + " ANALOG_WORKFLOW_RECIPIENT_DECEASED : " + hasAnalogWorkflow + " NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST : " + hasNotificationCancelledDocumentCreationRequest
                            );
                            return Mono.error(new NotificationReworkValidationException(
                                    NotificationReworkError.builder()
                                            .cause(NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT.getCause())
                                            .description(errorDescription)
                                            .build()
                            ));
                        }
                        return Mono.just(info);
                    } else {
                        return Mono.error(new NotificationReworkValidationException(
                                NotificationReworkError.builder()
                                        .cause(NotificationReworkErrorCause.INVALID_NOTIFICATION_STATUS.getCause())
                                        .description(NotificationReworkErrorCause.INVALID_NOTIFICATION_STATUS.getErrorDetails())
                                        .build()
                        ));
                    }
                });
    }

    private Mono<NotificationReworkInfo> checkNotificationTimelineAndStatusForMulti(NotificationReworkInfo info) {
        return Mono.just(info);
    }

    private int getRecIndexFromAction(Action action) {
        String recIndex = ((NotificationReworkValidationDetails) action.getDetails()).getReworkrecIndex();
        return Integer.parseInt(recIndex.substring(recIndex.lastIndexOf(REC_INDEX)) + 1);
    }

    @Nullable
    private Instant retrieveViewedDate(String iun, Integer recIndex) {
        //FIND TIMELINE ELEMENT
        Instant viewedDate = timelineUtils.getNotificationViewCreationRequest(iun, recIndex).map(notificationViewCreationRequestTimelineElem -> {
            if(notificationViewCreationRequestTimelineElem.getDetails() instanceof NotificationViewedCreationRequestDetailsInt notificationViewedCreationRequestDetails) {
                return notificationViewedCreationRequestDetails.getEventTimestamp();
            }
            return null;
        }).orElse(null);

        log.info("Viewed date iun={} recIndex={} viewedDate={}", iun, recIndex, viewedDate);

        return viewedDate;
    }

    @Nullable
    private Instant retrieveRefinementDate(String iun, Integer recIndex) {
        // recupero la refinementDate dall'element dello schedule refinement
        Instant refinementDate = timelineUtils.getScheduleRefinement(iun, recIndex).map(scheduleRefinementTimelineElem -> {
            if(scheduleRefinementTimelineElem.getDetails() instanceof ScheduleRefinementDetailsInt scheduleRefinementTimelineElementDetails) {
                return scheduleRefinementTimelineElementDetails.getSchedulingDate();
            }
            return null;
        }).orElse(null);

        if (refinementDate == null)
            log.error("Schedule refinement not found iun={} recIndex={}", iun, recIndex);
        else
            log.info("Schedule refinement date iun={} recIndex={} refinementDate={}", iun, recIndex, refinementDate);

        return refinementDate;
    }

}
