package it.pagopa.pn.deliverypushworkflow.middleware.pnclient.emdintegration;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.api.MessageApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageRequestBodyDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.emd.integration.model.SendMessageResponseDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class PnEmdIntegrationClientImplTest {

    private MessageApi messageApi;
    private PnEmdIntegrationClientImpl pnEmdIntegrationClient;

    @BeforeEach
    void setup() {
        messageApi = Mockito.mock(MessageApi.class);
        pnEmdIntegrationClient = new PnEmdIntegrationClientImpl(messageApi);
    }

    @Test
    void sendMessage_successfulResponse() {
        SendMessageRequestBodyDto request = new SendMessageRequestBodyDto();
        SendMessageResponseDto expectedResponse = new SendMessageResponseDto();
        expectedResponse.setOutcome(SendMessageResponseDto.OutcomeEnum.OK);
        Mockito.when(messageApi.sendMessage(Mockito.any(SendMessageRequestBodyDto.class)))
                .thenReturn(Mono.just(expectedResponse));

        SendMessageResponseDto actualResponse = pnEmdIntegrationClient.sendMessage(request);
        Assertions.assertEquals(expectedResponse, actualResponse);
    }

    @Test
    void sendMessage_exceptionThrown() {
        SendMessageRequestBodyDto request = new SendMessageRequestBodyDto();
        Mockito.when(messageApi.sendMessage(Mockito.any(SendMessageRequestBodyDto.class)))
                .thenReturn(Mono.error(new RuntimeException("Test exception")));

        SendMessageResponseDto actualResponse = pnEmdIntegrationClient.sendMessage(request);
        Assertions.assertEquals(SendMessageResponseDto.OutcomeEnum.NO_CHANNELS_ENABLED, actualResponse.getOutcome());
    }
}