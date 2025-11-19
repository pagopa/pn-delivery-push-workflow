package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.raddalt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.awspring.cloud.autoconfigure.config.parameterstore.ParameterStoreAutoConfiguration;
import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import it.pagopa.pn.commons.pnclients.RestTemplateFactory;
import it.pagopa.pn.deliverypushworkflow.MockAWSObjectsTest;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.config.msclient.RaddAltApiConfigurator;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.CheckCoverageRequest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.CheckCoverageResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.SearchMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery-push-workflow.radd-alt-base-url=http://localhost:9999"
})
@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration(exclude = {
        SqsAutoConfiguration.class,
        ParameterStoreAutoConfiguration.class
})
@ContextConfiguration(classes = {
        RaddAltClientImpl.class,
        PnDeliveryPushWorkflowConfigs.class,
        RestTemplateFactory.class,
        RaddAltApiConfigurator.class})
class RaddAltClientImplTest extends MockAWSObjectsTest {
    @Autowired
    private RaddAltClient raddAltClient;

    @Test
    void checkCoverageSuccess() throws JsonProcessingException {
        try (ClientAndServer ignored = ClientAndServer.startClientAndServer(9999);
             MockServerClient mockServerClient = new MockServerClient("localhost", 9999)) {


            SearchMode searchMode = SearchMode.LIGHT;
            LocalDate searchDate = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toLocalDate();

            CheckCoverageRequest checkCoverageRequest = new CheckCoverageRequest();
            checkCoverageRequest.setPr("pr");
            checkCoverageRequest.setCity("city");
            checkCoverageRequest.setCap("cap");
            checkCoverageRequest.setCountry("country");

            CheckCoverageResponse checkCoverageResponse = new CheckCoverageResponse();
            checkCoverageResponse.setHasCoverage(true);
            String path = "/radd-net-private/api/v1/coverages/check";

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());

            String requestJson = mapper.writeValueAsString(checkCoverageRequest);
            String responseJson = mapper.writeValueAsString(checkCoverageResponse);

            mockServerClient
                    .when(request()
                            .withMethod("POST")
                            .withPath(path)
                            .withQueryStringParameter("search_mode", String.valueOf(searchMode))
                            .withQueryStringParameter("search_date", String.valueOf(searchDate))
                            .withBody(requestJson)
                    )
                    .respond(response()
                            .withStatusCode(200)
                            .withBody(responseJson)
                            .withContentType(MediaType.APPLICATION_JSON)
                    );

            Assertions.assertDoesNotThrow(() -> raddAltClient.checkCoverage(searchMode, checkCoverageRequest, searchDate));
        }
    }

    @Test
    void checkCoverageBadRequest() throws JsonProcessingException {
        try (ClientAndServer ignored = ClientAndServer.startClientAndServer(9999);
             MockServerClient mockServerClient = new MockServerClient("localhost", 9999)) {

            SearchMode searchMode = SearchMode.LIGHT;
            LocalDate searchDate = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toLocalDate();

            CheckCoverageRequest checkCoverageRequest = new CheckCoverageRequest();
            checkCoverageRequest.setPr("pr");
            checkCoverageRequest.setCity("city");
            checkCoverageRequest.setCap("cap");
            checkCoverageRequest.setCountry("country");

            String path = "/radd-net-private/api/v1/coverages/check";

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());

            String requestJson = mapper.writeValueAsString(checkCoverageRequest);

            mockServerClient
                    .when(request()
                            .withMethod("POST")
                            .withPath(path)
                            .withQueryStringParameter("search_mode", String.valueOf(searchMode))
                            .withQueryStringParameter("search_date", String.valueOf(searchDate))
                            .withBody(requestJson)
                    )
                    .respond(response()
                            .withStatusCode(400)
                            .withBody("{\"error\":\"Bad Request\"}")
                            .withContentType(MediaType.APPLICATION_JSON)
                    );

            Assertions.assertThrows(
                    it.pagopa.pn.commons.exceptions.PnHttpResponseException.class,
                    () -> raddAltClient.checkCoverage(searchMode, checkCoverageRequest, searchDate)
            );
        }
    }

}
