package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString
public class AnalogFailureWorkflowTimeoutDetailsInt extends CategoryTypeTimelineElementDetailsInt implements ElementTimestampTimelineElementDetails, RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private String generatedAarUrl;
    private Integer notificationCost;
    private Instant timeoutDate;

    public String toLog() {
        return String.format(
                "recIndex=%d, cost=%d timeoutDate=%s",
                recIndex,
                notificationCost,
                timeoutDate
        );
    }

    @Override
    public Instant getElementTimestamp() {
        return timeoutDate;
    }
}
