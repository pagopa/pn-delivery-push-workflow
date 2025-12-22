package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationStatus;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationStatusHistoryInvalidatedElementInt {
    private NotificationStatus status;
    private Instant activeFrom;
    private List<String> relatedTimelineElementIds; //per uso interno per il mapping entityToDto e dtoToEntity
    private List<TimelineElementInternal> relatedTimelineElements = new ArrayList<>(); //per uso b2b e web
}
