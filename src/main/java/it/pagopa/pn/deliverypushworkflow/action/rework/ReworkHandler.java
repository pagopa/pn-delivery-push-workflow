package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.rework.ReworkError;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReworkHandler {

    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationService notificationService;
    private final NotificationProcessCostService notificationProcessCostService;
    private final AttachmentUtils attachmentUtils;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;


    public void handleRework(Action action) {

        log.info("Start HandleRefinement - iun {} id {}", action.getIun(), action.getRecipientIndex());

        List<ReworkError> errorList = new ArrayList<>();

        this.checkNotificationStatus(errorList);
        this.checkNotificationTimeline(errorList);
        this.checkNotificationExpectedFinalStatusCOde(errorList);
        this.checkNotificationAttachments(errorList);

        //TODO Domanda non ho un chiamante a cui restituire il 200 o sbaglio?
        this.checkNotificationAddress(errorList);

        this.checkErrorList(errorList);

    }

    private void checkNotificationStatus(List<ReworkError> errorList) {}

    private void checkNotificationTimeline(List<ReworkError> errorList) {}

    private void checkNotificationExpectedFinalStatusCOde(List<ReworkError> errorList) {}

    private void checkNotificationAttachments(List<ReworkError> errorList) {}

    private void checkNotificationAddress(List<ReworkError> errorList) {}

    private void checkErrorList(List<ReworkError> errorList) {}

}
