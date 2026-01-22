package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.utils;

import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkRequestedDetails;
import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkValidationDetails;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkError;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkOperationEnum;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventAction;

import java.util.List;

import static it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkErrorCause.REWORK_REQUESTED_PHASE_ERROR;

public class NotificationReworkUtils {

    public NotificationReworkUtils() {
    }

    public static ReworkRequestEventAction getReworkRequestEventAction(List<NotificationReworkError> errorList, NotificationReworkValidationDetails detail, Action action) {
        ReworkRequestEventAction reworkRequest = new ReworkRequestEventAction();
        reworkRequest.setError(errorList);
        reworkRequest.setIun(action.getIun());
        reworkRequest.setReworkId(detail.getReworkId());
        reworkRequest.setOperation(NotificationReworkOperationEnum.ERROR.name());
        return reworkRequest;
    }

    public static ReworkRequestEventAction getReworkRequestEventAction(String message, NotificationReworkRequestedDetails detail, Action action) {
        ReworkRequestEventAction reworkRequest = new ReworkRequestEventAction();
        reworkRequest.setError( List.of(NotificationReworkError.builder()
                .cause(REWORK_REQUESTED_PHASE_ERROR.getCause())
                .description(REWORK_REQUESTED_PHASE_ERROR.getErrorDetails() + ":" + message)
                .build()));
        reworkRequest.setIun(action.getIun());
        reworkRequest.setReworkId(detail.getReworkId());
        reworkRequest.setOperation(NotificationReworkOperationEnum.ERROR.name());
        return reworkRequest;
    }
}
