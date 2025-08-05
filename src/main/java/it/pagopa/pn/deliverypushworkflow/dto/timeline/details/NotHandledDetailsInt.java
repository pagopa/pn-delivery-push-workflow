package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString
public class NotHandledDetailsInt extends CategoryTypeTimelineElementDetailsInt implements RecipientRelatedTimelineElementDetails{
  public static final String PAPER_MESSAGE_NOT_HANDLED_CODE = "001";
  public static final String PAPER_MESSAGE_NOT_HANDLED_REASON = "Paper message not handled";

  private int recIndex;
  private String reasonCode;
  private String reason;

  public String toLog() {
    return String.format(
            "recIndex=%d reasonCode=%s reason=%s",
            recIndex,
            reasonCode,
            reason
    );
  }
}

