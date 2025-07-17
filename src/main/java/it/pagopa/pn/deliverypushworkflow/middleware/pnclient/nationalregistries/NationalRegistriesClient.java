package it.pagopa.pn.deliverypushworkflow.middleware.pnclient.nationalregistries;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypushworkflow.dto.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.nationalregistries.model.CheckTaxIdOKDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.nationalregistries.model.PhysicalAddressesRequestBodyDto;

import java.time.Instant;
import java.util.List;

public interface NationalRegistriesClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_NATIONAL_REGISTRIES;
    String GET_DIGITAL_GENERAL_ADDRESS = "GET DIGITAL GENERAL ADDRESS";
    String CHECK_TAX_ID = "CHECK TAX ID";

    String GET_PHYSICAL_ADDRESSES = "GET PHYSICAL ADDRESSES";

    void sendRequestForGetDigitalAddress(String taxId, String recipientType, String correlationId, Instant notificationSentAt);

    CheckTaxIdOKDto checkTaxId(String taxId);

    List<NationalRegistriesResponse> sendRequestForGetPhysicalAddresses(PhysicalAddressesRequestBodyDto physicalAddressesRequestBody);
}
 