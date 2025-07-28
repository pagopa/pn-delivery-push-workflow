package it.pagopa.pn.deliverypushworkflow.action.details;

import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecipientsWorkflowDetails implements ActionDetails {

  private String quickAccessLinkToken;
}
