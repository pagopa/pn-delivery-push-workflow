package it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool;

import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkError;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class ReworkUpdateRequestEventAction {
    private String iun;
    private String reworkId;
    private List<NotificationReworkError> error;
    private String operation;
}
