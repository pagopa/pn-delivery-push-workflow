package it.pagopa.pn.deliverypushworkflow.action.details;

import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.SequenceItemInternal;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationReworkUpdateDetails implements ActionDetails {
    private String reworkId;
    private String reworkAttempt;
    private String reworkRecIndex;
    private List<SequenceItemInternal> reworkExpectedStatusCodes;
    private String reworkExpectedDeliveryFailureCause;
    private String reworkExpectedFinalStatus;
}
