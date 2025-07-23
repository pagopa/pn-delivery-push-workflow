package it.pagopa.pn.deliverypushworkflow.dto.legalfacts;

import it.pagopa.pn.deliverypushworkflow.legalfacts.AarTemplateType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PdfInfo {
    private String key;
    private int numberOfPages;
    private AarTemplateType aarTemplateType;
}
