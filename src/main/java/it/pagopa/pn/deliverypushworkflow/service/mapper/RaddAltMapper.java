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
        if(physicalAddressInt.getAddress() != null){
            checkCoverageRequest.setAddressRow(physicalAddressInt.getAddress());
        }
        if(physicalAddressInt.getAddressDetails() != null){
            checkCoverageRequest.setAddressRow2(physicalAddressInt.getAddressDetails());
        }
        if (physicalAddressInt.getMunicipality() != null) {
            checkCoverageRequest.setCity(physicalAddressInt.getMunicipality());
        }
        if(physicalAddressInt.getMunicipalityDetails() != null){
            checkCoverageRequest.setCity2(physicalAddressInt.getMunicipalityDetails());
        }
        if(physicalAddressInt.getProvince() != null){
            checkCoverageRequest.setPr(physicalAddressInt.getProvince());
        }
        if(physicalAddressInt.getForeignState() != null){
            checkCoverageRequest.setCountry(physicalAddressInt.getForeignState());
        }
        if(physicalAddressInt.getFullname() != null){
            checkCoverageRequest.setNameRow2(physicalAddressInt.getFullname());
        }
        return checkCoverageRequest;
    }
}
