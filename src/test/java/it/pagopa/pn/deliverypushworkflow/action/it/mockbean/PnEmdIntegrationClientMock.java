package it.pagopa.pn.deliverypushworkflow.action.it.mockbean;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageRequestBody;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.emdintegration.PnEmdIntegrationClient;

public class PnEmdIntegrationClientMock implements PnEmdIntegrationClient {

    @Override
    public SendMessageResponse sendMessage(SendMessageRequestBody sendMessageRequest) {
        SendMessageResponse sendMessageResponse = new SendMessageResponse();
        sendMessageResponse.setOutcome(SendMessageResponse.OutcomeEnum.NO_CHANNELS_ENABLED);
        return sendMessageResponse;
    }
}
