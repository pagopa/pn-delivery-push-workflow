package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationStatusHistoryElement;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
public class NotificationTimelineReworkedDetailsInt extends CategoryTypeTimelineElementDetailsInt implements RecipientRelatedTimelineElementDetails, TimelineElementDetailsInt {
    private int recIndex;
    private Integer sentAttemptMade;
    private List<NotificationStatusHistoryElement> invalidatedTimelineAndStatusHistory;

    @Override
    public String toLog() {
        return String.format(
                "NotificationTimelineReworkedDetailsInt{recIndex=%d, sentAttemptMade=%d, reason='%s', categoryType='%s'}",
                recIndex,
                sentAttemptMade
        );
    }

}
