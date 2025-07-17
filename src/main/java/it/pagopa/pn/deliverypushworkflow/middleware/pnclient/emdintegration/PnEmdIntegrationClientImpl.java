package it.pagopa.pn.deliverypushworkflow.middleware.pnclient.emdintegration;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.api.MessageApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageRequestBodyDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponseDto;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@CustomLog
@Component
public class PnEmdIntegrationClientImpl extends CommonBaseClient implements PnEmdIntegrationClient {

    private final MessageApi messageApi;

    public SendMessageResponseDto sendMessage(SendMessageRequestBodyDto sendMessageRequest) {
        log.logInvokingExternalDownstreamService(CLIENT_NAME, SEND_MESSAGE);
        try {
            return messageApi.sendMessage(sendMessageRequest)
                    .block();
        } catch (Exception e) {
            log.logInvokationResultDownstreamFailed("Error sending message to EMD, fallback with NO_CHANNELS_ENABLED", e.getMessage());
            SendMessageResponseDto response = new SendMessageResponseDto();
            response.setOutcome(SendMessageResponseDto.OutcomeEnum.NO_CHANNELS_ENABLED);
            return response;
        }
    }
}
