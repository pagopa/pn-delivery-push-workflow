package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@Setter
@ToString
public class NotificationCancelledDetailsInt implements TimelineElementDetailsInt {

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