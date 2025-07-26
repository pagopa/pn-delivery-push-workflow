package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@Setter
@ToString
public class NotificationCancellationRequestDetailsInt implements TimelineElementDetailsInt {

    private String cancellationRequestId;

    public String toLog() {
        return String.format("cancellationRequestId=%s", cancellationRequestId);
    }
}
