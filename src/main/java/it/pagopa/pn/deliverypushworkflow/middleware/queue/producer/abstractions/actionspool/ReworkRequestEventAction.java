package it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool;

import it.pagopa.pn.deliverypushworkflow.dto.rework.ReworkError;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class ReworkRequestEventAction {
    private String iun;
    private String reworkId;
    private List<ReworkError> error;
    private String operation;
}
