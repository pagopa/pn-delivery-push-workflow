package it.pagopa.pn.deliverypushworkflow.dto.rework;

import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
public class NotificationReworkInfo {
    private List<NotificationReworkError> errorList = new ArrayList<>();
    private Action action;
    private Set<TimelineElementInternal> timeline;
    private String requestId;
}
