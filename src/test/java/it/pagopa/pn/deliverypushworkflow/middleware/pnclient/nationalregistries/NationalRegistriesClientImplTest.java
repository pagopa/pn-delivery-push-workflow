package it.pagopa.pn.deliverypushworkflow.middleware.pnclient.nationalregistries;

import it.pagopa.pn.deliverypushworkflow.dto.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.nationalregistries.api.AddressApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.nationalregistries.api.AgenziaEntrateApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.nationalregistries.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
        Mockito.when(addressApi.getAddresses(Mockito.eq("PF"), Mockito.any(AddressRequestBodyDto.class), Mockito.eq("pn-delivery-push")))
                        .thenReturn(Mono.just(new AddressOKDto().correlationId("002")));
        
        publicRegistry.sendRequestForGetDigitalAddress("001", "PF", "002", Instant.now());

        Mockito.verify(addressApi, Mockito.times(1)).getAddresses(Mockito.eq("PF"), Mockito.any(AddressRequestBodyDto.class), Mockito.eq("pn-delivery-push"));
    }

    @Test
    void sendRequestForGetDigitalAddressKO() {
        Mockito.when(addressApi.getAddresses(Mockito.eq("PF"), Mockito.any(AddressRequestBodyDto.class), Mockito.eq("pn-delivery-push")))
                .thenReturn(Mono.error(WebClientResponseException.create(502, "bad Gateway", null, null, Charset.defaultCharset())));

        Assertions.assertThrows(WebClientResponseException.BadGateway.class,
                () -> publicRegistry.sendRequestForGetDigitalAddress("001", "PF", "002", Instant.now()));
        Mockito.verify(addressApi, Mockito.times(1)).getAddresses(Mockito.eq("PF"), Mockito.any(AddressRequestBodyDto.class), Mockito.eq("pn-delivery-push"));
    }

    @Test
    void checkTaxId() {
        //GIVEN
        final String taxIdTest = "TaxIdTest";

        CheckTaxIdOKDto checkTaxIdOK = new CheckTaxIdOKDto()
                .taxId(taxIdTest)
                .isValid(true);
        Mockito.when(agenziaEntrateApi.checkTaxId(Mockito.any(CheckTaxIdRequestBodyDto.class)))
                .thenReturn(Mono.just(checkTaxIdOK));

        //WHEN
        CheckTaxIdOKDto checkTaxIdOKResponse = publicRegistry.checkTaxId(taxIdTest);
        
        //THEN
        Assertions.assertNotNull(checkTaxIdOKResponse);
        Assertions.assertEquals(taxIdTest, checkTaxIdOKResponse.getTaxId());
        Assertions.assertEquals(Boolean.TRUE, checkTaxIdOKResponse.getIsValid());

    }

    @Test
    void sendRequestForGetPhysicalAddressesOK() {
        PhysicalAddressesRequestBodyDto requestBody = new PhysicalAddressesRequestBodyDto();
        requestBody.setCorrelationId("test-correlation-id");
        requestBody.setReferenceRequestDate(Instant.now());
        requestBody.setAddresses(createRecipientAddressRequestBodyList());

        PhysicalAddressesResponseDto response = new PhysicalAddressesResponseDto();
        response.setCorrelationId("test-correlation-id");
        response.setAddresses(getPhysicalAddressResponseList());

        Mockito.when(addressApi.getPhysicalAddresses(Mockito.any(PhysicalAddressesRequestBodyDto.class)))
                .thenReturn(Mono.just(response));

        List<NationalRegistriesResponse> result = publicRegistry.sendRequestForGetPhysicalAddresses(requestBody);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("Test Address", result.get(0).getPhysicalAddress().getAddress());
        Mockito.verify(addressApi, Mockito.times(1)).getPhysicalAddresses(Mockito.any(PhysicalAddressesRequestBodyDto.class));
    }

    @Test
    void sendRequestForGetPhysicalAddressesKO() {
        PhysicalAddressesRequestBodyDto requestBody = new PhysicalAddressesRequestBodyDto();
        requestBody.setCorrelationId("test-correlation-id");

        Mockito.when(addressApi.getPhysicalAddresses(Mockito.any(PhysicalAddressesRequestBodyDto.class)))
                .thenReturn(Mono.error(WebClientResponseException.create(502, "Bad Gateway", null, null, Charset.defaultCharset())));

        Assertions.assertThrows(WebClientResponseException.BadGateway.class,
                () -> publicRegistry.sendRequestForGetPhysicalAddresses(requestBody));
        Mockito.verify(addressApi, Mockito.times(1)).getPhysicalAddresses(Mockito.any(PhysicalAddressesRequestBodyDto.class));
    }

    public List<RecipientAddressRequestBodyDto> createRecipientAddressRequestBodyList() {
        List<RecipientAddressRequestBodyDto> addresses = new ArrayList<>();

        RecipientAddressRequestBodyDto address1 = new RecipientAddressRequestBodyDto();
        address1.setRecIndex(0);
        address1.setTaxId("TaxId1");
        address1.setRecipientType(RecipientAddressRequestBodyDto.RecipientTypeEnum.PF);

        RecipientAddressRequestBodyDto address2 = new RecipientAddressRequestBodyDto();
        address2.setRecIndex(1);
        address2.setTaxId("TaxId2");
        address2.setRecipientType(RecipientAddressRequestBodyDto.RecipientTypeEnum.PG);

        addresses.add(address1);
        addresses.add(address2);

        return addresses;
    }

    private List<PhysicalAddressResponseDto> getPhysicalAddressResponseList() {
        List<PhysicalAddressResponseDto> physicalAddressResponses = new ArrayList<>();

        PhysicalAddressResponseDto address1 = new PhysicalAddressResponseDto();
        address1.setRegistry("ANPR");
        address1.setRecIndex(0);
        address1.setPhysicalAddress(setPhysicalAddressForResponse(address1));

        PhysicalAddressResponseDto address2 = new PhysicalAddressResponseDto();
        address2.setRegistry("REGISTRO_IMPRESE");
        address2.setRecIndex(1);
        address2.setPhysicalAddress(setPhysicalAddressForResponse(address2));

        physicalAddressResponses.add(address1);
        physicalAddressResponses.add(address2);

        return physicalAddressResponses;
    }

    public PhysicalAddressDto setPhysicalAddressForResponse(PhysicalAddressResponseDto response) {
        PhysicalAddressDto physicalAddress = new PhysicalAddressDto();
        physicalAddress.setAddress("Test Address");
        physicalAddress.setMunicipality("Municipality");
        physicalAddress.setZip("zip");
        physicalAddress.setProvince("province");
        return physicalAddress;
    }

}