package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkValidationDetails;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.rework.NotificationReworkError;
import it.pagopa.pn.deliverypushworkflow.dto.rework.NotificationReworkErrorCause;
import it.pagopa.pn.deliverypushworkflow.dto.rework.NotificationReworkInfo;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.NotificationViewedCreationRequestDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.ScheduleRefinementDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.exceptions.NotificationReworkValidationException;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReworkValidationHandler {

    private static final String REC_INDEX = "RECINDEX_";
    private static final String ATTEMPT_ID = "ATTEMPT_";
    private static final String STATUS_EFFECTIVE_DATE = "EFFECTIVE_DATE";
    private static final String STATUS_VIEWED = "VIEWED";
    private static final String STATUS_RETURNED_TO_SENDER = "RETURNED_TO_SENDER";

    private final List<String> MONO_REC_NOTIFICATION_VALID_STATUS = List.of("EFFECTIVE_DATE", "RETURNED_TO_SENDER", "VIEWED");
    private final List<String> MULTI_REC_NOTIFICATION_VALID_STATUS = List.of("DELIVERING", "DELIVERED", "EFFETCTIVE_DATE", "VIEWED", "RETURNED_TO_SENDER", "UNREACHABLE");
    private final String REC_INDEX = "RECINDEX_";
    private final String ATTEMPT_0 = "ATTEMPT_0";
    private final String ATTEMPT_1 = "ATTEMPT_1";
    private final String KO = "KO";


    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;

    private Mono<NotificationReworkInfo> checkNotificationStatusAndThrow(NotificationReworkInfo info) {
        return Mono.just(notificationService.getNotificationByIun(info.getAction().getIun()))
                .flatMap(notification -> {
                    info.setRecipientSize(notification.getRecipients().size());
                    int recIndex = getRecIndexFromAction(info.getAction());
                    if (notification.getRecipients().size() > recIndex) {
                        return checkNotificationStatus(notification, info);
                    } else {
                        return Mono.error(new NotificationReworkValidationException(NotificationReworkError.builder().cause(NotificationReworkErrorCause.INVALID_RECINDEX.getCause()).description(NotificationReworkErrorCause.INVALID_RECINDEX.getErrorDetails()).build()));
                    }
                });
    }

    private Mono<NotificationReworkInfo> checkNotificationExpectedFinalStatusCodeAndThrow(NotificationReworkInfo info) {
        return Mono.just(info.getTimeline())
                .flatMap(timeline -> {
                    boolean hasAttempt0 = timeline.stream().anyMatch(timelineElement -> timelineElement.getElementId().contains(ATTEMPT_0));
                    boolean hasAttempt1 = timeline.stream().anyMatch(timelineElement -> timelineElement.getElementId().contains(ATTEMPT_1));
                    String expectedAttempt = ((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkAttempt();
                    String expectedStatus = ((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkexpectedFinalStatus();

                    if (hasAttempt0 && hasAttempt1 && ATTEMPT_0.equals(expectedAttempt) && KO.equals(expectedStatus)) {
                        return Mono.error(new NotificationReworkValidationException(NotificationReworkError.builder().cause(NotificationReworkErrorCause.INVALID_EXPECTED_STATUS_CODE.getCause()).description(String.format(NotificationReworkErrorCause.INVALID_EXPECTED_STATUS_CODE.getErrorDetails(), expectedStatus, expectedAttempt)).build()));
                    }
                    return Mono.just(info);
                });
    }

    private int getRecIndexFromAction(Action action) {
        String recIndex = ((NotificationReworkValidationDetails) action.getDetails()).getReworkrecIndex();
        return Integer.parseInt(recIndex.substring(recIndex.lastIndexOf(REC_INDEX)) + 1);
    }

    private Mono<NotificationReworkInfo> checkNotificationStatus(NotificationInt notification, NotificationReworkInfo info) {
        NotificationHistoryResponse response = timelineService.getTimelineAndStatusHistory(notification.getIun(), notification.getRecipients().size(), notification.getSentAt());
        info.setNotificationStatus(response.getNotificationStatus().getValue());
        if ((notification.getRecipients().size() == 1 && !MONO_REC_NOTIFICATION_VALID_STATUS.contains(response.getNotificationStatus().getValue())) ||
                (notification.getRecipients().size() > 1 && !MULTI_REC_NOTIFICATION_VALID_STATUS.contains(response.getNotificationStatus().getValue()))) {

            String errorMessage = notification.getRecipients().size() > 1 ?
                    String.format(NotificationReworkErrorCause.INVALID_NOTIFICATION_STATUS.getErrorDetails(), response.getNotificationStatus().getValue(), MULTI_REC_NOTIFICATION_VALID_STATUS) :
                    String.format(NotificationReworkErrorCause.INVALID_NOTIFICATION_STATUS.getErrorDetails(), response.getNotificationStatus().getValue(), MONO_REC_NOTIFICATION_VALID_STATUS);
            return Mono.error(new NotificationReworkValidationException(NotificationReworkError.builder().cause(NotificationReworkErrorCause.INVALID_NOTIFICATION_STATUS.getCause()).description(errorMessage).build()));
        }
        return Mono.just(info);
    }

    private Mono<NotificationReworkInfo> checkNotificationTimelineAndThrow(NotificationReworkInfo info) {
        String recIndex = ((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkrecIndex() == null ? REC_INDEX+"0" : ((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkrecIndex();
        String attempt = ((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkAttempt();

        return Flux.fromIterable(info.getTimeline())
                .filter(timelineElement -> timelineElement.getElementId().contains(attempt))
                .switchIfEmpty(Mono.error(new NotificationReworkValidationException(NotificationReworkError.builder().cause(NotificationReworkErrorCause.INVALID_ATTEMPT_ID.getCause()).description(NotificationReworkErrorCause.INVALID_ATTEMPT_ID.getErrorDetails()).build())))
                .filter(timelineElement -> timelineElement.getElementId().contains(recIndex))
                .collectList()
                .flatMap(timeline -> {
                    if (timeline.isEmpty()) {
                        return Mono.error(new NotificationReworkValidationException(NotificationReworkError.builder().cause(NotificationReworkErrorCause.INVALID_RECINDEX.getCause()).description(NotificationReworkErrorCause.INVALID_RECINDEX.getErrorDetails()).build()));
                    }
                    info.setFilteredTimeline(timeline);
                    return Mono.just(info);
                })
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
        List<TimelineElementInternal> timeline = info.getFilteredTimeline().stream().toList();

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
        List<TimelineElementInternal> timeline = info.getFilteredTimeline().stream().toList();

        if (MULTI_REC_NOTIFICATION_VALID_STATUS.contains(status)) {
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

    private int getRecIndexFromAction(Action action) {
        String recIndex = ((NotificationReworkValidationDetails) action.getDetails()).getReworkrecIndex();
        return Integer.parseInt(recIndex.substring(recIndex.lastIndexOf(REC_INDEX)) + 1);
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