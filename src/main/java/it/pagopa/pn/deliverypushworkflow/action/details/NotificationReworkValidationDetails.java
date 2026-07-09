package it.pagopa.pn.deliverypushworkflow.action.details;

import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.ReworkRequestTypeEnum;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationReworkValidationDetails implements ActionDetails {
    private String reworkId;
    private String reworkAttempt;
    private String reworkPcRetry;
    private String reworkRecIndex;
    private String reworkExpectedFinalStatus;
    private List<String> elementsToInvalidate;
    private ReworkRequestTypeEnum requestType;
}
