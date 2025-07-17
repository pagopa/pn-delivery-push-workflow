package it.pagopa.pn.deliverypushworkflow.middleware.pnclient.userattributes;

import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.userattributes.api.CourtesyApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.userattributes.api.LegalApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.userattributes.model.CourtesyChannelTypeDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.userattributes.model.CourtesyDigitalAddressDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.userattributes.model.LegalChannelTypeDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.userattributes.model.LegalDigitalAddressDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

class UserAttributesClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PnDeliveryPushWorkflowConfigs cfg;

    @Mock
    private CourtesyApi courtesyApi;

    @Mock
    private LegalApi legalApi;

    private UserAttributesClientImpl client;

    @BeforeEach
    void setup() {
        client = new UserAttributesClientImpl(courtesyApi, legalApi);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void getLegalAddressBySender() {
        LegalDigitalAddressDto legalDigitalAddress = new LegalDigitalAddressDto();
        legalDigitalAddress.setValue("indirizzo@prova.com");
        legalDigitalAddress.setChannelType(LegalChannelTypeDto.PEC);
        legalDigitalAddress.recipientId("001");
        legalDigitalAddress.senderId("001");

        List<LegalDigitalAddressDto> listLegalDigitalAddresses = Collections.singletonList(legalDigitalAddress);

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));

        Mockito.when(legalApi.getLegalAddressBySenderWithHttpInfo("001", "001")).thenReturn(ResponseEntity.ok(listLegalDigitalAddresses));

        List<LegalDigitalAddressDto> response = client.getLegalAddressBySender("001", "001");

        Assertions.assertEquals(response, listLegalDigitalAddresses);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void getCourtesyAddressBySender() {

        CourtesyDigitalAddressDto courtesyDigitalAddress = new CourtesyDigitalAddressDto();
        courtesyDigitalAddress.setValue("indirizzo@prova.com");
        courtesyDigitalAddress.setChannelType(CourtesyChannelTypeDto.EMAIL);
        courtesyDigitalAddress.recipientId("001");
        courtesyDigitalAddress.senderId("001");

        List<CourtesyDigitalAddressDto> courtesyDigitalAddressList = Collections.singletonList(courtesyDigitalAddress);

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));

        Mockito.when(courtesyApi.getCourtesyAddressBySenderWithHttpInfo("001", "001")).thenReturn(ResponseEntity.ok(courtesyDigitalAddressList));

        List<CourtesyDigitalAddressDto> response = client.getCourtesyAddressBySender("001", "001");

        Assertions.assertEquals(response, courtesyDigitalAddressList);
    }
}