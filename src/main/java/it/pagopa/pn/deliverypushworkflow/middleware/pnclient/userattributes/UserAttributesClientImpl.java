package it.pagopa.pn.deliverypushworkflow.middleware.pnclient.userattributes;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.userattributes.api.CourtesyApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.userattributes.api.LegalApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.userattributes.model.CourtesyDigitalAddressDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.userattributes.model.LegalDigitalAddressDto;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@CustomLog
@RequiredArgsConstructor
@Component
public class UserAttributesClientImpl implements UserAttributesClient {
    private final CourtesyApi courtesyApi;
    private final LegalApi legalApi;
    
    @Override
    public List<LegalDigitalAddressDto> getLegalAddressBySender(String recipientId, String senderId) {
        log.logInvokingExternalService(CLIENT_NAME, GET_DIGITAL_PLATFORM_ADDRESS);

        ResponseEntity<List<LegalDigitalAddressDto>> resp = legalApi.getLegalAddressBySenderWithHttpInfo(recipientId, senderId);
        return resp.getBody();
    }

    @Override
    public List<CourtesyDigitalAddressDto> getCourtesyAddressBySender(String recipientId, String senderId) {
        log.logInvokingExternalService(CLIENT_NAME, GET_COURTESY_ADDRESS);
        
        ResponseEntity<List<CourtesyDigitalAddressDto>> resp = courtesyApi.getCourtesyAddressBySenderWithHttpInfo(recipientId, senderId);
        return resp.getBody();
    }
}
