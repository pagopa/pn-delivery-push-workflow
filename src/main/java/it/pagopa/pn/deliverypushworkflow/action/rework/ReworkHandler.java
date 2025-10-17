package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkRequestedDetails;
import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkValidationDetails;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.rework.ReworkError;
import it.pagopa.pn.deliverypushworkflow.dto.rework.ReworkInfo;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushWorkflowGenericException;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.actionmanager.api.ActionApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.actionmanager.model.ActionType;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.actionmanager.model.NewAction;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.api.CheckAddressApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.model.CheckAddressResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventAction;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventPool;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventType;
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
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReworkHandler {

    private final CheckAddressApi checkAddressApi;
    private final ActionApi actionManagerApi;
    private final TimelineService timelineService;
    private final ReworkRequestEventPool reworkRequestEventPool;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;
    private final TimelineUtils timelineUtils;

    public void handleRework(Action action) {
        log.info("Start handleRework - iun {} id {}", action.getIun(), action.getRecipientIndex());
        ReworkInfo reworkInfo = new ReworkInfo();
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
                .flatMap(this::checkErrorList)
                .doOnError(e -> {
                    log.error("Errore durante handleRework per iun {}: {}", action.getIun(), e.getMessage(), e);
                    this.checkErrorList(reworkInfo);
                })
                .doOnSuccess(v -> log.info("handleRework completato per iun {}", action.getIun()))
                .block();

    }

    private Mono<ReworkInfo> checkNotificationCancelledAndThrow(ReworkInfo externalInfo) {
        return Mono.just(externalInfo)
                .flatMap(info -> {
                    if (timelineUtils.checkIsNotificationCancellationRequested(info.getAction().getIun())) {
                        info.getErrorList().add(ReworkError.builder().cause("NOTIFICATION_CANCELLED").description("La notifica è stata cancellata").build());
                        return Mono.error(new PnDeliveryPushWorkflowGenericException("La notifica è stata cancellata", HttpStatus.INTERNAL_SERVER_ERROR.toString()));
                    }
                    return Mono.just(info);
                });
    }

    private Mono<ReworkInfo> checkNotificationStatusAndThrow(ReworkInfo info) { return Mono.just(info);}

    private Mono<ReworkInfo> retrieveTimeline(ReworkInfo info) {
        info.setTimeline(timelineService.getTimeline(info.getAction().getIun(), false));
        return Mono.just(info);
    }

    private Mono<ReworkInfo> checkNotificationTimelineAndThrow(ReworkInfo info) { return Mono.just(info);}

    private Mono<ReworkInfo> checkNotificationExpectedFinalStatusCodeAndThrow(ReworkInfo info) { return Mono.just(info);}

    private Mono<ReworkInfo> checkNotificationAttachments(ReworkInfo info) { return Mono.just(info);}

    private Mono<ReworkInfo> computeRequestId(ReworkInfo info) {
        log.debug("computeRequestId per iun {}", info.getAction().getIun());
        return Flux.fromIterable(info.getTimeline())
                .filter(timelineElement -> timelineElement.getCategory().equals(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE))
                .filter(timelineElement -> timelineElement.getElementId().endsWith("ATTEMPT_"+((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkAttempt()))
                .flatMap(timelineElementInternal -> {
                    String requestId = timelineElementInternal.getElementId() + "." + ((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkpcRetry();
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

    private Mono<ReworkInfo> checkNotificationAddress(ReworkInfo externalInfo) {
        log.debug("checkNotificationAddress per iun {}, requestId {}", externalInfo.getAction().getIun(), externalInfo.getRequestId());
        return Mono.just(externalInfo)
                .flatMap(info -> {
                    int range = pnDeliveryPushWorkflowConfigs.getReworkTTLAddressRange();
                    CheckAddressResponse response = checkAddressApi.checkAddress(info.getRequestId());
                    if (Boolean.TRUE.equals(response.getFound())) {
                        log.info("Indirizzo trovato per requestId {}", info.getRequestId());
                        if (response.getEndValidity() != null && response.getEndValidity().minus(range, ChronoUnit.DAYS).isBefore(Instant.now())) {
                            log.warn("Indirizzo per requestId {} scade tra meno di {} giorni", info.getRequestId(), range);
                            info.getErrorList().add(ReworkError.builder().cause("INVALID_ANALOG_ADDRESS").description("Indirizzo trovato ma scade tra " + range + " giorni").build());
                        }
                    } else {
                        log.warn("Indirizzo non trovato per requestId {}", info.getRequestId());
                        info.getErrorList().add(ReworkError.builder().cause("EXPIRED_ANALOG_ADDRESS").description("Indirizzo non trovato").build());
                    }
                    return Mono.just(info);
                });
    }

    private Mono<Void> checkErrorList(ReworkInfo externalInfo) {
        log.debug("checkErrorList per iun {}", externalInfo.getAction().getIun());
        return Mono.just(externalInfo)
                .flatMap(info -> {
                    if (info.getErrorList().isEmpty()) {
                        log.info("Nessun errore trovato, inserisco nuova action per iun {}", info.getAction().getIun());
                        actionManagerApi.insertAction(getNewAction(info.getAction(), info.getRequestId()));
                    } else {
                        log.error("Errori trovati per iun {}: {}", info.getAction().getIun(), info.getErrorList());
                        reworkRequestEventPool.scheduleFutureAction(getReworkRequestEventAction(info), ReworkRequestEventType.NOTIFICATION_REWORK_REQUESTED);
                    }
                    return Mono.empty();
                });
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

    private static @NotNull ReworkRequestEventAction getReworkRequestEventAction(ReworkInfo info) {
        ReworkRequestEventAction reworkRequest = new ReworkRequestEventAction();
        reworkRequest.setError(info.getErrorList());
        reworkRequest.setIun(info.getAction().getIun());
        reworkRequest.setReworkId(((NotificationReworkValidationDetails) info.getAction().getDetails()).getReworkId());
        reworkRequest.setOperation("ERROR");
        return reworkRequest;
    }

}
