package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypushworkflow.MockAWSObjectsTest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.SentNotificationV25;
import org.junit.jupiter.api.AfterAll;
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

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery-push-workflow.delivery-base-url=http://localhost:9998",
})
class PnDeliveryClientReactiveImplTestIT extends MockAWSObjectsTest {
    @Autowired
    private PnDeliveryClientReactive client;
    
    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startMockServer() {

        mockServer = startClientAndServer(9998);
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }

    @Test
    void removeAllNotificationCostsByIun() throws JsonProcessingException {
        //Given
        String iun ="iunTest";
        SentNotificationV25 notification = new SentNotificationV25();
        notification.setIun(iun);

        String path = "/delivery-private/notification-cost/{iun}"
                .replace("{iun}",iun);

        ObjectMapper mapper = new ObjectMapper();
        String respjson = mapper.writeValueAsString(notification);
        mockServer.stop();
        mockServer = startClientAndServer(9998);
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("DELETE")
                        .withPath(path))
                .respond(response()
                        .withBody(respjson)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));

        Mono<Void> response = client.removeAllNotificationCostsByIun(iun);

        Assertions.assertDoesNotThrow(() ->  response.block());
    }

    @Test
    void removeAllNotificationCostsByIunError(){
        //Given
        String iun ="iunTest1";
        SentNotificationV25 notification = new SentNotificationV25();
        notification.setIun(iun);

        String path = "/delivery-private/notification-cost/{iun}"
                .replace("{iun}",iun);
        mockServer.stop();
        mockServer = startClientAndServer(9998);
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("DELETE")
                        .withPath(path))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(400));

        client.removeAllNotificationCostsByIun(iun).onErrorResume(
                ex -> {
                    Assertions.assertNotNull(ex);
                    return Mono.empty();
                }
        );
    }
}