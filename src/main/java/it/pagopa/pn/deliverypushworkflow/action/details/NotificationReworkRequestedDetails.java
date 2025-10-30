package it.pagopa.pn.deliverypushworkflow.action.details;

import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationReworkRequestedDetails implements ActionDetails {
    private String reworkrequestId;
    private String attempt;
    private String recIndex;
    private String reworkId;
    private Instant createdAt;
}