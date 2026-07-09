package it.pagopa.pn.deliverypushworkflow.action.details;

import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.ReworkRequestTypeEnum;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationReworkRequestedDetails implements ActionDetails {
    private String reworkRequestId;
    private String reworkAttempt;
    private String reworkRecIndex;
    private String reworkId;
    private Instant createdAt;
    private List<String> elementsToInvalidate;
    private ReworkRequestTypeEnum requestType;
}