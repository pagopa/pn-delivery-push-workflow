package it.pagopa.pn.deliverypushworkflow.service.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.CheckCoverageRequest;
import org.springframework.stereotype.Component;

@Component
public class RaddAltMapper {
    private final ObjectMapper objectMapper;

    public RaddAltMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

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
