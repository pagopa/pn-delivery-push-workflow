package it.pagopa.pn.deliverypushworkflow.middleware.pnclient.externalregistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypushworkflow.MockAWSObjectsTest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.PaymentsInfoForRecipientDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequestDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResponseDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResultDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery-push-workflow.external-registry-base-url=http://localhost:9998",
})
class PnExternalRegistriesClientReactiveImplTest extends MockAWSObjectsTest {
    @Autowired
    private PnExternalRegistriesClientReactive client;

    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startMockServer() {
        mockServer = startClientAndServer(9998);
    }
    
    @Test
    void updateNotificationCost() throws JsonProcessingException {
        //Given
        String path = "/ext-registry-private/cost-update";

        ObjectMapper mapper = new ObjectMapper();

        UpdateNotificationCostRequestDto request = new UpdateNotificationCostRequestDto();
        final String iun = "iun";
        request.setIun(iun);
        request.setNotificationStepCost(100);
        final PaymentsInfoForRecipientDto paymentsInfoForRecipient = new PaymentsInfoForRecipientDto()
                .creditorTaxId("testcred")
                .noticeCode("testCod")
                .recIndex(0);
        request.setPaymentsInfoForRecipients(Collections.singletonList(paymentsInfoForRecipient));
        request.setUpdateCostPhase(UpdateNotificationCostRequestDto.UpdateCostPhaseEnum.NOTIFICATION_CANCELLED);
        
        String requestJson = mapper.writeValueAsString(request);

        UpdateNotificationCostResponseDto response = new UpdateNotificationCostResponseDto();
        response.setUpdateResults(Collections.singletonList(
                new UpdateNotificationCostResultDto()
                        .creditorTaxId(paymentsInfoForRecipient.getCreditorTaxId())
                        .noticeCode(paymentsInfoForRecipient.getNoticeCode())
                        .recIndex(paymentsInfoForRecipient.getRecIndex())
        ));
        
        String responseJson = mapper.writeValueAsString(response);

        try (MockServerClient mockServerClient = new MockServerClient("localhost", 9998)) {
            mockServerClient
                    .when(request()
                            .withMethod("POST")
                            .withPath(path)
                            .withBody(requestJson)
                    )
                    .respond(response()
                            .withBody(responseJson)
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withStatusCode(200)
                    );

            Mono<UpdateNotificationCostResponseDto> responseMono = client.updateNotificationCost(request);

            Assertions.assertNotNull(responseMono);
            UpdateNotificationCostResponseDto responseExpected = responseMono.block();
            Assertions.assertEquals(response, responseExpected);
        }
    }
    
}