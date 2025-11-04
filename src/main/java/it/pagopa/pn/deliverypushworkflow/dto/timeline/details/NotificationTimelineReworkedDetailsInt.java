package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationStatusHistoryElementV26;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
public class NotificationTimelineReworkedDetailsInt implements RecipientRelatedTimelineElementDetails, TimelineElementDetailsInt {
    private int recIndex;
    private Integer sentAttemptMade;
    private List<NotificationStatusHistoryElementV26> invalidatedTimelineAndStatusHistory;
    private String reason;
    private String categoryType;

    @Override
    public String toLog() {
        return String.format(
                "NotificationTimelineReworkedDetailsInt{recIndex=%d, sentAttemptMade=%d, reason='%s', categoryType='%s'}",
                recIndex,
                sentAttemptMade,
                reason,
                categoryType
        );
    }

}
