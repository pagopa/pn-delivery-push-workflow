package it.pagopa.pn.deliverypushworkflow.middleware.pnclient.externalregistry;


import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry.api.RootSenderIdApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry.api.SendIoMessageApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry.model.SendMessageRequestDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry.model.SendMessageResponseDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

class PnExternalRegistryClientImplTest {
    
    @Mock
    private SendIoMessageApi sendIoMessageApi;

    @Mock
    private RootSenderIdApi rootSenderIdApi;

    private PnExternalRegistryClientImpl client;

    @BeforeEach
    void setup() {
        client = new PnExternalRegistryClientImpl(sendIoMessageApi,rootSenderIdApi);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void sendIOMessage() {

        SendMessageRequestDto request = new SendMessageRequestDto();
        request.setIun("001");

        SendMessageResponseDto response = new SendMessageResponseDto();
        response.setId("001");
        
        Mockito.when(sendIoMessageApi.sendIOMessageWithHttpInfo(request)).thenReturn(ResponseEntity.ok(response));

        SendMessageResponseDto resp = client.sendIOMessage(request);

        Assertions.assertEquals("001", resp.getId());
    }

}