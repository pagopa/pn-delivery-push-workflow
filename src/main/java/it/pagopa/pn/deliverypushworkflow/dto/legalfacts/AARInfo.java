package it.pagopa.pn.deliverypushworkflow.dto.legalfacts;

import it.pagopa.pn.deliverypushworkflow.legalfacts.AarTemplateType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class AARInfo {
    private byte[] bytesArrayGeneratedAar;
    private AarTemplateType templateType;
}
