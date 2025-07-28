package it.pagopa.pn.deliverypushworkflow.service.mapper;

import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.UsedServicesInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.UsedServices;

public class UsedServicesMapper {
    private UsedServicesMapper() {
    }

    public static UsedServicesInt externalToInternal(UsedServices external) {
        return external != null ? UsedServicesInt.builder()
                .physicalAddressLookUp(external.getPhysicalAddressLookup())
                .build() : null;
    }
}
