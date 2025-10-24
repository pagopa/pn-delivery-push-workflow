package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkRequestedDetails;
import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkValidationDetails;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkError;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkErrorCause;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkInfo;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.NotificationViewedCreationRequestDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.ScheduleRefinementDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.exceptions.NotificationReworkValidationException;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.actionmanager.api.ActionApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.actionmanager.model.ActionType;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.actionmanager.model.NewAction;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.api.CheckAddressApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.model.CheckAddressResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventAction;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventPool;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventType;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.SafeStorageService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkConstant.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReworkValidationHandler {

    private final List<String> MONO_REC_NOTIFICATION_VALID_STATUS = List.of("EFFECTIVE_DATE", "RETURNED_TO_SENDER", "VIEWED");
    private final List<String> MULTI_REC_NOTIFICATION_VALID_STATUS = List.of("DELIVERING", "DELIVERED", "EFFECTIVE_DATE", "VIEWED", "RETURNED_TO_SENDER", "UNREACHABLE");
    private final String REC_INDEX = "RECINDEX_";

    private final CheckAddressApi checkAddressApi;
    private final ActionApi actionManagerApi;
    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final ReworkRequestEventPool reworkRequestEventPool;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;
    private final SafeStorageService safeStorageService;

    public void handleNotificationRework(Action action) {
        log.info("Start handleRework - iun {} id {}", action.getIun(), action.getRecipientIndex());
        NotificationReworkInfo reworkInfo = new NotificationReworkInfo();
        reworkInfo.setAction(action);

        Mono.just(reworkInfo)
                .flatMap(this::checkNotificationCancelledAndThrow)
                .flatMap(this::checkNotificationStatusAndThrow)
                .flatMap(this::retrieveTimeline)
                .flatMap(this::checkNotificationTimelineAndThrow)
                .flatMap(this::checkNotificationExpectedFinalStatusCodeAndThrow)
                .flatMap(this::checkNotificationAttachments)
                .flatMap(this::computeRequestId)
                .flatMap(this::checkNotificationAddress)
                .flatMap(info -> this.checkErrorList(info.getErrorList(), info.getAction(), info.getRequestId()))
                .onErrorResume(NotificationReworkValidationException.class, e -> {
                    log.error("Errore durante handleRework per iun {}: {}", action.getIun(), e.getMessage(), e);
                    return this.checkErrorList(e.getErrors(), action, null);
                })
                .doOnSuccess(v -> log.info("handleRework completato per iun {}", action.getIun()))
                .block();

    }

    private Mono<NotificationReworkInfo> checkNotificationCancelledAndThrow(NotificationReworkInfo externalInfo) {
        return Mono.just(externalInfo)
                .flatMap(info -> {
                    if (timelineUtils.checkIsNotificationCancellationRequested(info.getAction().getIun())) {
                        return Mono.error(new NotificationReworkValidationException(NotificationReworkError.builder().cause(NotificationReworkErrorCause.NOTIFICATION_CANCELLED.getCause()).description(NotificationReworkErrorCause.NOTIFICATION_CANCELLED.getErrorDetails()).build()));
                    }
                    return Mono.just(info);
                });
    }

    private Mono<NotificationReworkInfo> checkNotificationStatusAndThrow(NotificationReworkInfo info) {
        return Mono.just(notificationService.getNotificationByIun(info.getAction().getIun()))
                .doOnNext(info::setNotification)
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

    private Mono<NotificationReworkInfo> retrieveTimeline(NotificationReworkInfo info) {
        info.setTimeline(timelineService.getTimeline(info.getAction().getIun(), false));
        return Mono.just(info);
    }

    private Mono<NotificationReworkInfo> checkNotificationExpectedFinalStatusCodeAndThrow(NotificationReworkInfo info) {
        return Mono.just(info.getTimeline())
                .flatMap(timeline -> {
                    boolean hasAttempt0 = timeline.stream().anyMatch(timelineElement -> timelineElement.getElementId().contains(ATTEMPT_0));
                    boolean hasAttempt1 = timeline.stream().anyMatch(timelineElement -> timelineElement.getElementId().contains(ATTEMPT_1));
                    String expectedAttempt = ((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkAttempt();
                    String expectedStatus = ((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkExpectedFinalStatus();

                    if (hasAttempt0 && hasAttempt1 && ATTEMPT_0.equals(expectedAttempt) && KO.equals(expectedStatus)) {
                        return Mono.error(new NotificationReworkValidationException(NotificationReworkError.builder().cause(NotificationReworkErrorCause.INVALID_EXPECTED_STATUS_CODE.getCause()).description(String.format(NotificationReworkErrorCause.INVALID_EXPECTED_STATUS_CODE.getErrorDetails(), expectedStatus, expectedAttempt)).build()));
                    }
                    return Mono.just(info);
                });
    }

    private Mono<NotificationReworkInfo> checkNotificationAttachments(NotificationReworkInfo info) {
        return Flux.fromIterable(info.getNotification().getDocuments())
                .flatMap(document -> safeStorageService.getFile(document.getRef().getKey(), true, false))
                .filter(response -> response.getRetentionUntil().minusDays(pnDeliveryPushWorkflowConfigs.getNotificationReworkDocumentExpiringRange()).isBefore(OffsetDateTime.now()))
                .map(response -> NotificationReworkError.builder()
                        .cause(NotificationReworkErrorCause.INVALID_ATTACHMENT.getCause())
                        .description(String.format(NotificationReworkErrorCause.INVALID_ATTACHMENT.getErrorDetails(), response.getKey(), response.getRetentionUntil()))
                        .build())
                .onErrorResume(PnHttpResponseException.class, ex ->
                        ex.getStatusCode() == HttpStatus.GONE.value()
                                ? Mono.just(NotificationReworkError.builder()
                                .cause(NotificationReworkErrorCause.EXPIRED_ATTACHMENT.getCause())
                                .description(NotificationReworkErrorCause.EXPIRED_ATTACHMENT.getErrorDetails())
                                .build())
                                : Mono.empty()
                )
                .collectList()
                .map(errors -> {
                    info.getErrorList().addAll(errors);
                    return info;
                })
                .then(Mono.just(info));
    }

    private Mono<NotificationReworkInfo> computeRequestId(NotificationReworkInfo info) {
        log.debug("computeRequestId per iun {}", info.getAction().getIun());
        return Flux.fromIterable(info.getTimeline())
                .filter(timelineElement -> timelineElement.getCategory().equals(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE))
                .filter(timelineElement -> timelineElement.getElementId().endsWith("ATTEMPT_"+((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkAttempt()))
                .flatMap(timelineElementInternal -> {
                    String requestId = timelineElementInternal.getElementId() + "." + ((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkPcRetry();
                    info.setRequestId(requestId);
                    log.info("RequestId calcolato: {}", requestId);
                    return Mono.just(info);
                })
                .switchIfEmpty(Mono.fromRunnable(() -> {
                    log.warn("Nessun elemento timeline trovato per iun {}", info.getAction().getIun());
                    info.setRequestId(StringUtils.EMPTY);
                }))
                .then(Mono.just(info));
    }

    private Mono<NotificationReworkInfo> checkNotificationAddress(NotificationReworkInfo externalInfo) {
        log.debug("checkNotificationAddress per iun {}, requestId {}", externalInfo.getAction().getIun(), externalInfo.getRequestId());
        return Mono.just(externalInfo)
                .flatMap(info -> {
                    int range = pnDeliveryPushWorkflowConfigs.getReworkTTLAddressRange();
                    CheckAddressResponse response = checkAddressApi.checkAddress(info.getRequestId());
                    if (Boolean.TRUE.equals(response.getFound())) {
                        log.info("Indirizzo trovato per requestId {}", info.getRequestId());
                        if (response.getEndValidity() != null && response.getEndValidity().minus(range, ChronoUnit.DAYS).isBefore(Instant.now())) {
                            log.warn("Indirizzo per requestId {} scade tra meno di {} giorni", info.getRequestId(), range);
                            info.getErrorList().add(NotificationReworkError.builder().cause(NotificationReworkErrorCause.INVALID_ANALOG_ADDRESS.getCause()).description(String.format(NotificationReworkErrorCause.INVALID_ANALOG_ADDRESS.getErrorDetails(), DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(response.getEndValidity()))).build());
                        }
                    } else {
                        log.warn("Indirizzo non trovato per requestId {}", info.getRequestId());
                        info.getErrorList().add(NotificationReworkError.builder().cause(NotificationReworkErrorCause.EXPIRED_ANALOG_ADDRESS.getCause()).description(NotificationReworkErrorCause.EXPIRED_ANALOG_ADDRESS.getErrorDetails()).build());
                    }
                    return Mono.just(info);
                });
    }

    private Mono<Void> checkErrorList(List<NotificationReworkError> errorList, Action action, String requestId) {
        log.debug("checkErrorList per iun {}: {}", action.getIun(), errorList);
        return Mono.just(errorList)
                .flatMap(errors -> {
                    if (errors.isEmpty()) {
                        log.info("Nessun errore trovato, inserisco nuova action per iun {}", action.getIun());
                        actionManagerApi.insertAction(getNewAction(action, requestId));
                    } else {
                        log.error("Errori trovati per iun {}: {}", action, errors);
                        reworkRequestEventPool.scheduleFutureAction(getReworkRequestEventAction(errors, action), ReworkRequestEventType.NOTIFICATION_REWORK_REQUESTED);
                    }
                    return Mono.empty();
                });
    }

    private Mono<NotificationReworkInfo> checkNotificationStatus(NotificationInt notification, NotificationReworkInfo info) {
        NotificationHistoryResponse response = timelineService.getTimelineAndStatusHistory(notification.getIun(), notification.getRecipients().size(), notification.getSentAt());
        info.setNotificationStatus(Objects.nonNull(response.getNotificationStatus()) ? response.getNotificationStatus().getValue() : null);
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
        String recIndex = ((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkRecIndex();
        String attempt = ((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkAttempt();

        return Mono.just(info.getTimeline().stream().filter(timelineElementInternal -> timelineElementInternal.getElementId().contains(attempt)).collect(Collectors.toSet()))
                .filter(timelineElementInternals -> !timelineElementInternals.isEmpty())
                .switchIfEmpty(fail(NotificationReworkErrorCause.INVALID_ATTEMPT_ID, NotificationReworkErrorCause.INVALID_ATTEMPT_ID.getErrorDetails()))
                .map(timelineElement -> timelineElement.stream().filter(timelineElementInternal -> timelineElementInternal.getElementId().contains(recIndex)).collect(Collectors.toSet()))
                .switchIfEmpty(fail(NotificationReworkErrorCause.INVALID_RECINDEX, NotificationReworkErrorCause.INVALID_RECINDEX.getErrorDetails()))
                .doOnNext(info::setFilteredTimeline)
                .flatMap(timeline -> checkNotificationTimeline(info, recIndex, attempt))
                .thenReturn(info);
    }

    private Mono<Void> checkNotificationTimeline(NotificationReworkInfo info, String recIndex, String attempt) {

        String status = info.getNotificationStatus();
        Set<TimelineElementInternal> timeline = info.getFilteredTimeline();

        if (!containsCategory(timeline, TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)) {
            log.warn("La timeline non contiene l'elemento SEND_ANALOG_FEEDBACK necessario a procedere con la richiesta di invalidazione per lo iun: [{}] , recIndex: [{}] e attemptId: [{}]", info.getAction().getIun(), recIndex, attempt);
            return fail(NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT, "SEND_ANALOG_FEEDBACK assente");
        }

        if (STATUS_VIEWED.equals(status) && !containsCategory(timeline, TimelineElementCategoryInt.REFINEMENT)) {
            Instant refinementDate = timeline.stream()
                    .filter(e -> e.getCategory() == TimelineElementCategoryInt.SCHEDULE_REFINEMENT)
                    .map(TimelineElementInternal::getDetails)
                    .filter(details -> details instanceof ScheduleRefinementDetailsInt)
                    .map(details -> (ScheduleRefinementDetailsInt) details)
                    .findFirst()
                    .map(ScheduleRefinementDetailsInt::getSchedulingDate)
                    .orElse(null);

            Instant viewedDate = timeline.stream()
                    .filter(e -> e.getCategory() == TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST)
                    .map(TimelineElementInternal::getDetails)
                    .filter(details -> details instanceof NotificationViewedCreationRequestDetailsInt)
                    .map(details -> (NotificationViewedCreationRequestDetailsInt) details)
                    .findFirst()
                    .map(NotificationViewedCreationRequestDetailsInt::getEventTimestamp)
                    .orElse(null);

            if (Objects.nonNull(viewedDate) && Objects.nonNull(refinementDate) && viewedDate.isAfter(refinementDate)) {
                log.warn("Il refinement della notifica è in corso, non è possibile procedere alla richiesta di invalidazione per lo iun: [{}] , recIndex: [{}] e attemptId: [{}]", info.getAction().getIun(), recIndex, attempt);
                return fail(NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT, "Refinement in corso");
            }
        }

        if (!STATUS_VIEWED.equals(status) && !containsCategory(timeline, TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED) && !containsCategory(timeline, TimelineElementCategoryInt.REFINEMENT)) {
            log.warn("La timeline non contiene gli elementi finali (REFINEMENT o ANALOG_WORKFLOW_RECIPIENT_DECEASED) necessari a procedere con la richiesta di invalidazione per lo iun: [{}] , recIndex: [{}] e attemptId: [{}]", info.getAction().getIun(), recIndex, attempt);
            return fail(NotificationReworkErrorCause.INVALID_TIMELINE_ELEMENT, "REFINEMENT e ANALOG_WORKFLOW_RECIPIENT_DECEASED assenti");
        }

        return Mono.empty();
    }


    private boolean containsCategory(Set<TimelineElementInternal> timeline, TimelineElementCategoryInt category) {
        return timeline.stream().anyMatch(e -> e.getCategory() == category);
    }

    private int getRecIndexFromAction(Action action) {
        String recIndex = ((NotificationReworkValidationDetails) action.getDetails()).getReworkRecIndex();
        return Integer.parseInt(recIndex.substring((recIndex.lastIndexOf(REC_INDEX) + 9)));
    }

    private <T> Mono<T> fail(NotificationReworkErrorCause cause, String details) {
        return Mono.error(new NotificationReworkValidationException(
                NotificationReworkError.builder().cause(cause.getCause()).description(details).build()));
    }

    private static @NotNull NewAction getNewAction(Action action, String requestId) {
        NewAction newAction = new NewAction();
        newAction.setActionId(((NotificationReworkValidationDetails) action.getDetails()).getReworkId());
        newAction.setIun(action.getIun());
        newAction.setType(ActionType.NOTIFICATION_REWORK_REQUESTED);
        newAction.setNotBefore(Instant.now());
        NotificationReworkRequestedDetails request = new NotificationReworkRequestedDetails();
        request.setReworkId(action.getActionId());
        request.setReworkrequestId(requestId);
        newAction.setDetails(request.toString());
        return newAction;
    }

    private static @NotNull ReworkRequestEventAction getReworkRequestEventAction(List<NotificationReworkError> errorList, Action action) {
        ReworkRequestEventAction reworkRequest = new ReworkRequestEventAction();
        reworkRequest.setError(errorList);
        reworkRequest.setIun(action.getIun());
        reworkRequest.setReworkId(((NotificationReworkValidationDetails) action.getDetails()).getReworkId());
        reworkRequest.setOperation("ERROR");
        return reworkRequest;
    }
}