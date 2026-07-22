package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.emdintegration;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.api.MessageApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageRequestBody;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@CustomLog
@Component
public class PnEmdIntegrationClientImpl extends CommonBaseClient implements PnEmdIntegrationClient {

    private final MessageApi messageApi;

    public SendMessageResponse sendMessage(SendMessageRequestBody sendMessageRequest) {
        log.logInvokingExternalService(CLIENT_NAME, SEND_MESSAGE);
        return messageApi.sendMessage(sendMessageRequest)
                .block();
    }
}
