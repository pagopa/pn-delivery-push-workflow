package it.pagopa.pn.deliverypushworkflow.middleware.pnclient.emdintegration;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageRequestBodyDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponseDto;

public interface PnEmdIntegrationClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_EMD_INTEGRATION;
    String SEND_MESSAGE = "sendMessage";
    SendMessageResponseDto sendMessage(SendMessageRequestBodyDto sendMessageRequest);
}
