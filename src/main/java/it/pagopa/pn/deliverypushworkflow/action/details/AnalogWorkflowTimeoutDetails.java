package it.pagopa.pn.deliverypushworkflow.action.details;

import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalogWorkflowTimeoutDetails implements ActionDetails {
    private int sentAttemptMade;
}
