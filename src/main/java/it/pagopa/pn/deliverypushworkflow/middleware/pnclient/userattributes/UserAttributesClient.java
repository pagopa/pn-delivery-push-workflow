package it.pagopa.pn.deliverypushworkflow.middleware.pnclient.userattributes;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.userattributes.model.CourtesyDigitalAddressDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.userattributes.model.LegalDigitalAddressDto;


import java.util.List;

public interface UserAttributesClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_USER_ATTRIBUTES;
    String GET_DIGITAL_PLATFORM_ADDRESS = "GET DIGITAL PLATFORM ADDRESS";
    String GET_COURTESY_ADDRESS = "GET COURTESY ADDRESS";

    List<LegalDigitalAddressDto> getLegalAddressBySender(String internalId, String senderId);

    List<CourtesyDigitalAddressDto> getCourtesyAddressBySender(String internalId, String senderId);
}
