package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.datavault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.MockAWSObjectsTest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.BaseRecipientDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.DenominationDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.AddressDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.MandateDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.RecipientAddressesDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.RecipientType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery-push-workflow.data-vault-base-url=http://localhost:9998"
})
class PnDataVaultClientReactiveImplTestIT extends MockAWSObjectsTest {
    @Autowired
    @SuppressWarnings("unused")
    private PnDataVaultClientReactiveImpl client;
    
    private static ClientAndServer mockServer;

    @AfterEach
    void stopMockServer() {
        if (mockServer != null && mockServer.isRunning()) {
            mockServer.stop();
        }
    }
    
    @Test
    void getRecipientDenominationByInternalId() throws JsonProcessingException {
        mockServer = startClientAndServer(9998);

        //Given
        String path = "/datavault-private/v1/recipients/internal";

        ObjectMapper mapper = new ObjectMapper();

        String internalId = "internalIdTest";

        BaseRecipientDto responseDto = new BaseRecipientDto();
        responseDto.setDenomination("denomination");
        responseDto.setInternalId(internalId);
        responseDto.setTaxId("taxId");
        responseDto.setRecipientType(RecipientType.PF);
        
        String responseJson = mapper.writeValueAsString(responseDto);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                )
                .respond(response()
                        .withBody(responseJson)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200)
                );

        Flux<BaseRecipientDto> responseMono = client.getRecipientsDenominationByInternalId(List.of(internalId));
        Assertions.assertNotNull(responseMono);
        BaseRecipientDto response = responseMono.blockFirst();
        Assertions.assertEquals(responseDto, response);

        mockServer.stop();
    }

    @Test
    void getRecipientDenominationByInternalIdKo() {
        mockServer = startClientAndServer(9998);

        //Given
        String path = "/datavault-private/v1/recipients/internal";
        String internalId = "internalId";

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                )
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(400)
                );

        Flux<BaseRecipientDto> responseMono = client.getRecipientsDenominationByInternalId(List.of(internalId));
        Assertions.assertNotNull(responseMono);
        
        Assertions.assertThrows( PnInternalException.class, responseMono::blockFirst);
        
        mockServer.stop();
    }

    @Test
    void getMandatesByIdsOk() throws JsonProcessingException {
        mockServer = startClientAndServer(9998);

        //Given
        String path = "/datavault-private/v1/mandates";

        ObjectMapper mapper = new ObjectMapper();

        String mandateId = "mandateId";

        MandateDto responseDto = new MandateDto();
        responseDto.setMandateId(mandateId);
        DenominationDto info = new DenominationDto();
        info.setDestName("destName");
        info.setDestSurname("destSurname");
        responseDto.setInfo(info);

        String responseJson = mapper.writeValueAsString(responseDto);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                )
                .respond(response()
                        .withBody(responseJson)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200)
                );

        Flux<MandateDto> responseMono = client.getMandatesByIds(List.of(mandateId));
        Assertions.assertNotNull(responseMono);
        MandateDto response = responseMono.blockFirst();
        Assertions.assertEquals(responseDto, response);

        mockServer.stop();
    }

    @Test
    void getMandatesByIdsKo() {
        mockServer = startClientAndServer(9998);

        //Given
        String path = "/datavault-private/v1/mandates";
        String mandateId = "mandateId";

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                )
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(400)
                );

        Flux<MandateDto> responseMono = client.getMandatesByIds(List.of(mandateId));
        Assertions.assertNotNull(responseMono);

        Assertions.assertThrows( PnInternalException.class, responseMono::blockFirst);

        mockServer.stop();
    }

    @Test
    void updateRecipientAddressOk() throws JsonProcessingException {
        mockServer = startClientAndServer(9998);

        //Given
        String internalId = "internalIdTest";
        String addressId = "COURTESY_PLANNED#iun_01#0#EMAIL";
        String pathPattern = "/datavault-private/v1/recipients/internal/" + internalId + "/addresses/.*";

        MockServerClient mockServerClient = new MockServerClient("localhost", 9998);
        mockServerClient
                .when(request()
                        .withMethod("PUT")
                        .withPath(pathPattern)
                )
                .respond(response()
                        .withStatusCode(204)
                );

        Mono<Void> responseMono = client.updateRecipientAddress(internalId, addressId, BigDecimal.valueOf(86400), "congelato@test.it");
        Assertions.assertNotNull(responseMono);
        Assertions.assertDoesNotThrow(() -> { responseMono.block(); });

        HttpRequest[] recorded = mockServerClient.retrieveRecordedRequests(request().withMethod("PUT"));
        Assertions.assertEquals(1, recorded.length);
        // the address id carries '#', which must travel percent encoded or the path would be truncated
        Assertions.assertTrue(recorded[0].getPath().getValue().endsWith("/addresses/COURTESY_PLANNED%23iun_01%230%23EMAIL")
                        || recorded[0].getPath().getValue().endsWith("/addresses/" + addressId),
                "unexpected path: " + recorded[0].getPath().getValue());
        Assertions.assertEquals("86400", recorded[0].getFirstQueryStringParameter("ttl"));
        Assertions.assertEquals("congelato@test.it",
                new ObjectMapper().readTree(recorded[0].getBodyAsString()).get("value").asText());

        mockServer.stop();
    }

    @Test
    void updateRecipientAddressKo() {
        mockServer = startClientAndServer(9998);

        //Given
        String internalId = "internalIdTest";
        String addressId = "COURTESY_PLANNED#iun_01#0#EMAIL";

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("PUT")
                )
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(400)
                );

        Mono<Void> responseMono = client.updateRecipientAddress(internalId, addressId, BigDecimal.valueOf(86400), "congelato@test.it");
        Assertions.assertNotNull(responseMono);

        Assertions.assertThrows( PnInternalException.class, responseMono::block);

        mockServer.stop();
    }

    @Test
    void getRecipientAddressOk() throws JsonProcessingException {
        mockServer = startClientAndServer(9998);

        //Given
        String internalId = "internalIdTest";
        String addressId = "COURTESY_PLANNED#iun_01#0#EMAIL";
        String path = "/datavault-private/v1/recipients/internal/" + internalId + "/addresses";

        ObjectMapper mapper = new ObjectMapper();
        RecipientAddressesDto responseDto = new RecipientAddressesDto();
        responseDto.setAddresses(Map.of(
                addressId, new AddressDto().value("congelato@test.it"),
                "LEGAL#default#PEC", new AddressDto().value("altro@test.it")
        ));

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                )
                .respond(response()
                        .withBody(mapper.writeValueAsString(responseDto))
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200)
                );

        Mono<String> responseMono = client.getRecipientAddress(internalId, addressId);
        Assertions.assertNotNull(responseMono);
        Assertions.assertEquals("congelato@test.it", responseMono.block());

        mockServer.stop();
    }

    @Test
    void getRecipientAddressNotFound() throws JsonProcessingException {
        mockServer = startClientAndServer(9998);

        //Given
        String internalId = "internalIdTest";
        String path = "/datavault-private/v1/recipients/internal/" + internalId + "/addresses";

        ObjectMapper mapper = new ObjectMapper();
        RecipientAddressesDto responseDto = new RecipientAddressesDto();
        responseDto.setAddresses(Map.of("LEGAL#default#PEC", new AddressDto().value("altro@test.it")));

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                )
                .respond(response()
                        .withBody(mapper.writeValueAsString(responseDto))
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200)
                );

        Mono<String> responseMono = client.getRecipientAddress(internalId, "COURTESY_PLANNED#iun_01#0#EMAIL");
        Assertions.assertNotNull(responseMono);
        Assertions.assertNull(responseMono.block());

        mockServer.stop();
    }

    @Test
    void getRecipientAddressKo() {
        mockServer = startClientAndServer(9998);

        //Given
        String internalId = "internalIdTest";
        String path = "/datavault-private/v1/recipients/internal/" + internalId + "/addresses";

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                )
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(400)
                );

        Mono<String> responseMono = client.getRecipientAddress(internalId, "COURTESY_PLANNED#iun_01#0#EMAIL");
        Assertions.assertNotNull(responseMono);

        Assertions.assertThrows( PnInternalException.class, responseMono::block);

        mockServer.stop();
    }
}
