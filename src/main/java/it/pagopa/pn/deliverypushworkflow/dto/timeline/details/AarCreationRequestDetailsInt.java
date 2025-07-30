package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import it.pagopa.pn.deliverypushworkflow.legalfacts.AarTemplateType;
import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString
public class AarCreationRequestDetailsInt extends CategoryTypeTimelineElementDetailsInt implements RecipientRelatedTimelineElementDetails{
    private int recIndex;
    private String aarKey;
    private Integer numberOfPages; //Nota il campo potrà essere eliminato in futuro dal momento che il numero di pagine viene calcolato da paperChannel
    private AarTemplateType aarTemplateType;
    
    public String toLog() {
        return String.format(
                "recIndex=%d aarKey=%s numberOfPages=%s aarTemplateType=%s",
                recIndex,
                aarKey,
                numberOfPages,
                aarTemplateType
        );
    }
}
