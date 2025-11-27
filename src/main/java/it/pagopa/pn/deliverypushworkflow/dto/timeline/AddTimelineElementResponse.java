package it.pagopa.pn.deliverypushworkflow.dto.timeline;

import lombok.*;

@Getter
@Setter
@Data
@AllArgsConstructor
public class AddTimelineElementResponse {
    private String timelineElementId;
    private boolean isDuplicate;
}
