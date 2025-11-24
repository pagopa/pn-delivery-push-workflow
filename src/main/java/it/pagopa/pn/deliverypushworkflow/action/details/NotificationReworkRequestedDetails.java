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
    private String reworkRequestId;
    private String reworkAttempt;
    private String reworkRecIndex;
    private String reworkId;
    private Instant createdAt;
}