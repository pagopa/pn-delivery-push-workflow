package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkRequestedDetails;
import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkValidationDetails;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.rework.ReworkError;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.actionmanager.api.ActionApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.actionmanager.model.ActionType;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.actionmanager.model.NewAction;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.api.CheckAddressApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.api.PaperMessagesApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.model.CheckAddressResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventAction;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventPool;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventType;
import it.pagopa.pn.deliverypushworkflow.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.PaperChannelService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReworkHandler {

    private final CheckAddressApi checkAddressApi;
    private final ActionApi actionManagerApi;
    private final TimelineService timelineService;
    private final ReworkRequestEventPool reworkRequestEventPool;
    private final TimelineUtils timelineUtils;

    public void handleRework(Action action) {

        log.info("Start HandleRefinement - iun {} id {}", action.getIun(), action.getRecipientIndex());

        List<ReworkError> errorList = new ArrayList<>();

        Set<TimelineElementInternal> timeline = timelineService.getTimeline(action.getIun(), false);

        this.checkNotificationStatus(errorList);
        this.checkNotificationTimeline(errorList);
        this.checkNotificationExpectedFinalStatusCOde(errorList);
        this.checkNotificationAttachments(errorList);

        String requestId = this.computeRequestId(action, timeline);

        this.checkNotificationAddress(errorList, requestId);

        this.checkErrorList(errorList, action, requestId);

    }

    private void checkNotificationStatus(List<ReworkError> errorList) {}

    private void checkNotificationTimeline(List<ReworkError> errorList) {}

    private void checkNotificationExpectedFinalStatusCOde(List<ReworkError> errorList) {}

    private void checkNotificationAttachments(List<ReworkError> errorList) {}

    private void checkNotificationAddress(List<ReworkError> errorList, String requestId) {
        //TODO mettere il range in configurazione
        int range = 10;
        CheckAddressResponse response = checkAddressApi.checkAddress(requestId);
        if (Boolean.TRUE.equals(response.getFound())) {
            if (response.getEndValidity() != null && response.getEndValidity().minus(range, ChronoUnit.DAYS).isBefore(Instant.now())) {
                errorList.add(ReworkError.builder().cause("INVALID_ANALOG_ADDRESS").description("Indirizzo trovato ma scade tra" + range + " giorni").build());
            }
        } else {
            errorList.add(ReworkError.builder().cause("EXPIRED_ANALOG_ADDRESS").description("Indirizzo non trovato").build());
        }
    }

    private String computeRequestId(Action action, Set<TimelineElementInternal> timeline) {
        return timeline.stream()
                .filter(timelineElement -> timelineElement.getCategory().equals(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE))
                .filter(timelineElement -> timelineElement.getElementId().endsWith("ATTEMPT_"+((NotificationReworkValidationDetails) action.getDetails()).getReworkAttempt()))
                .findFirst()
                .orElse(new TimelineElementInternal())
                .getElementId() + "." + ((NotificationReworkValidationDetails) action.getDetails()).getReworkpcRetry();
    }

    private void checkErrorList(List<ReworkError> errorList, Action action, String requestId) {
        if (errorList.isEmpty()) {
            actionManagerApi.insertAction(getNewAction(action, requestId));
        } else {
            ReworkRequestEventAction reworkRequest = new ReworkRequestEventAction();
            reworkRequest.setError(errorList);
            reworkRequest.setIun(action.getIun());
            reworkRequest.setReworkId(((NotificationReworkValidationDetails) action.getDetails()).getReworkId());
            reworkRequest.setOperation("ERROR");
            reworkRequestEventPool.scheduleFutureAction(reworkRequest, ReworkRequestEventType.NOTIFICATION_REWORK_REQUESTED);
        }
    }

    private static @NotNull NewAction getNewAction(Action action, String requestId) {
        NewAction newAction = new NewAction();
        newAction.setActionId(((NotificationReworkRequestedDetails) action.getDetails()).getReworkId());
        newAction.setIun(action.getIun());
        newAction.setType(ActionType.NOTIFICATION_REWORK_REQUESTED);
        newAction.setNotBefore(Instant.now());
        NotificationReworkRequestedDetails request = new NotificationReworkRequestedDetails();
        request.setReworkId(action.getActionId());
        request.setReworkrequestId(requestId);
        newAction.setDetails(request.toString());
        return newAction;
    }

}
