package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@ToString
public class NotificationCancelledDetailsInt extends CategoryTypeTimelineElementDetailsInt implements TimelineElementDetailsInt {

    private int notificationCost;
    private List<Integer> notRefinedRecipientIndexes;

    public String toLog() {
        return String.format(
            "notificationCost=%d notRefinedRecipientIndexes=%s",
            notificationCost,
            notRefinedRecipientIndexes.toString()
        );
    }
}