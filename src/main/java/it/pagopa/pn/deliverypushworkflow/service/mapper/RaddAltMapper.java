package it.pagopa.pn.deliverypushworkflow.service.mapper;

import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.CheckCoverageRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class RaddAltMapper {

    public CheckCoverageRequest fromRequestIntToRequestExt(PhysicalAddressInt physicalAddressInt) {
        if (physicalAddressInt == null) return null;
        CheckCoverageRequest checkCoverageRequest = new CheckCoverageRequest();
        if (physicalAddressInt.getZip() != null) {
            checkCoverageRequest.setCap(physicalAddressInt.getZip());
        }
        if (physicalAddressInt.getMunicipality() != null) {
            checkCoverageRequest.setCity(physicalAddressInt.getMunicipality());
        }
        return checkCoverageRequest;
    }
}
