package it.pagopa.pn.deliverypushworkflow.middleware.pnclient.externalregistry;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry.model.SendMessageRequestDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry.model.SendMessageResponseDto;

public interface PnExternalRegistryClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_REGISTRIES;
    String SEND_IO_MESSAGE = "SEND MESSAGE TO IO";
    
    SendMessageResponseDto sendIOMessage(SendMessageRequestDto sendMessageRequest);
    String getRootSenderId(String senderId);
}
