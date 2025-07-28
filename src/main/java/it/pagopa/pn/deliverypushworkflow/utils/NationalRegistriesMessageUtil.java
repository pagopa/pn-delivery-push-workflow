package it.pagopa.pn.deliverypushworkflow.utils;

import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.nationalregistries.model.AddressSQSMessageDigitalAddressInner;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class NationalRegistriesMessageUtil {

    private NationalRegistriesMessageUtil(){}

    public static NationalRegistriesResponse buildPublicRegistryResponse(String correlationId, List<AddressSQSMessageDigitalAddressInner> digitalAddresses) {
        return NationalRegistriesResponse.builder()
                .correlationId(correlationId)
                .digitalAddress(mapToLegalDigitalAddressInt(digitalAddresses))
                .build();
    }

    private static LegalDigitalAddressInt mapToLegalDigitalAddressInt(List<AddressSQSMessageDigitalAddressInner> digitalAddresses) {
        if(CollectionUtils.isEmpty(digitalAddresses)) return null;

        return LegalDigitalAddressInt.builder()
                .address(digitalAddresses.get(0).getAddress())
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.valueOf(digitalAddresses.get(0).getType()))
                .build();
    }

}
