package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.nationalregistries;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.nationalregistries.api.AddressApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.nationalregistries.api.AgenziaEntrateApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.nationalregistries.model.AddressOK;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.nationalregistries.model.AddressRequestBody;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.time.Instant;

class NationalRegistriesClientImplTest {

    private NationalRegistriesClientImpl publicRegistry;
    private AddressApi addressApi = Mockito.mock(AddressApi.class);
    private AgenziaEntrateApi agenziaEntrateApi = Mockito.mock(AgenziaEntrateApi.class);

    @BeforeEach
    void setUp() {
        publicRegistry = new NationalRegistriesClientImpl(addressApi, agenziaEntrateApi);
    }

    @Test
    void sendRequestForGetDigitalAddressOK() {
        Mockito.when(addressApi.getAddresses(Mockito.eq("PF"), Mockito.any(AddressRequestBody.class), Mockito.eq("pn-delivery-push")))
                        .thenReturn(Mono.just(new AddressOK().correlationId("002")));
        
        publicRegistry.sendRequestForGetDigitalAddress("001", "PF", "002", Instant.now());

        Mockito.verify(addressApi, Mockito.times(1)).getAddresses(Mockito.eq("PF"), Mockito.any(AddressRequestBody.class), Mockito.eq("pn-delivery-push"));
    }

    @Test
    void sendRequestForGetDigitalAddressKO() {
        Mockito.when(addressApi.getAddresses(Mockito.eq("PF"), Mockito.any(AddressRequestBody.class), Mockito.eq("pn-delivery-push")))
                .thenReturn(Mono.error(WebClientResponseException.create(502, "bad Gateway", null, null, Charset.defaultCharset())));

        Assertions.assertThrows(WebClientResponseException.BadGateway.class,
                () -> publicRegistry.sendRequestForGetDigitalAddress("001", "PF", "002", Instant.now()));
        Mockito.verify(addressApi, Mockito.times(1)).getAddresses(Mockito.eq("PF"), Mockito.any(AddressRequestBody.class), Mockito.eq("pn-delivery-push"));
    }

}