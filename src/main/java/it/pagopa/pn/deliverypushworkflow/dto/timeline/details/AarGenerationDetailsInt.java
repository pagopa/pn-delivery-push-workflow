package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class AarGenerationDetailsInt implements RecipientRelatedTimelineElementDetails{
  private int recIndex;
  private String generatedAarUrl;
  private Integer numberOfPages; //Nota il campo potrà essere eliminato in futuro dal momento che il numero di pagine viene calcolato da paperChannel

  public String toLog() {
    return String.format(
            "recIndex=%d",
            recIndex
    );
  }
  
}