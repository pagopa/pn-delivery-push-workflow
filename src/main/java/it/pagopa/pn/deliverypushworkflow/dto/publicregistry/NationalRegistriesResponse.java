package it.pagopa.pn.deliverypushworkflow.dto.publicregistry;

import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class NationalRegistriesResponse {
    private String correlationId;
    private Integer recIndex;
    private String registry;
    private String error;
    private Integer errorStatus;
    private LegalDigitalAddressInt digitalAddress;
    private PhysicalAddressInt physicalAddress;
}
