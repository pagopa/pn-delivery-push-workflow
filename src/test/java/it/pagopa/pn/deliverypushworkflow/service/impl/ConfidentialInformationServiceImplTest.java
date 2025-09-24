package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.BaseRecipientDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.DenominationDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.MandateDto;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.datavault.PnDataVaultClientReactive;
import it.pagopa.pn.deliverypushworkflow.service.ConfidentialInformationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class ConfidentialInformationServiceImplTest {
    private ConfidentialInformationService confidentialInformationService;
    private PnDataVaultClientReactive pnDataVaultClientReactive;
    
    @BeforeEach
    void setup() {
        pnDataVaultClientReactive = Mockito.mock( PnDataVaultClientReactive.class );

        confidentialInformationService = new ConfidentialInformationServiceImpl(pnDataVaultClientReactive);

    }
    @Test
    void getRecipientInformationByInternalId() {
        //GIVEN
        String internalId = "internalId";
        String taxId = "testTaxId";
        String denomination = "denomination1";
        
        Flux<BaseRecipientDto> flux = Flux.just(BaseRecipientDto.builder()
                        .taxId(taxId)
                        .internalId(internalId)
                        .denomination(denomination)
                .build());
        Mockito.when(pnDataVaultClientReactive.getRecipientsDenominationByInternalId(Mockito.any())).thenReturn(flux);
        Mono<BaseRecipientDtoInt> monoBaseRec = confidentialInformationService.getRecipientInformationByInternalId(internalId);

        BaseRecipientDtoInt baseRecipientDto = monoBaseRec.block();
        Assertions.assertNotNull(baseRecipientDto);
        Assertions.assertEquals(taxId, baseRecipientDto.getTaxId());
        Assertions.assertEquals(denomination, baseRecipientDto.getDenomination());
    }

    @Test
    void getMandatesByIdsPf() {
        String mandateId = "mandateId";
        String destName = "Name";
        String destSurname = "Surname";
        String expectedDenomination = "Name Surname";
        RecipientTypeInt delegateType = RecipientTypeInt.PF;

        Flux<MandateDto> flux = Flux.just(MandateDto.builder()
                .mandateId(mandateId)
                .info(DenominationDto.builder()
                        .destName(destName)
                        .destSurname(destSurname)
                        .build())
                .build());
        Mockito.when(pnDataVaultClientReactive.getMandatesByIds(Mockito.any())).thenReturn(flux);

        Mono<BaseRecipientDtoInt> monoDelegateInfo = confidentialInformationService.getDelegateInformationByMandateId(mandateId, delegateType);

        BaseRecipientDtoInt baseRecipientDto = monoDelegateInfo.block();
        Assertions.assertNotNull(baseRecipientDto);
        Assertions.assertEquals(expectedDenomination, baseRecipientDto.getDenomination());
    }

    @Test
    void getMandatesByIdsPG() {
        String mandateId = "mandateId";
        String destBusinessName = "Fake Business Name";
        String expectedDenomination = "Fake Business Name";
        RecipientTypeInt delegateType = RecipientTypeInt.PG;

        Flux<MandateDto> flux = Flux.just(MandateDto.builder()
                .mandateId(mandateId)
                .info(DenominationDto.builder()
                        .destBusinessName(destBusinessName)
                        .build())
                .build());
        Mockito.when(pnDataVaultClientReactive.getMandatesByIds(Mockito.any())).thenReturn(flux);

        Mono<BaseRecipientDtoInt> monoDelegateInfo = confidentialInformationService.getDelegateInformationByMandateId(mandateId, delegateType);

        BaseRecipientDtoInt baseRecipientDto = monoDelegateInfo.block();
        Assertions.assertNotNull(baseRecipientDto);
        Assertions.assertEquals(expectedDenomination, baseRecipientDto.getDenomination());
    }

    @Test
    void getMandatesByIdsNotFound() {
        String mandateId = "mandateId";
        RecipientTypeInt delegateType = RecipientTypeInt.PF;

        Flux<MandateDto> flux = Flux.empty();
        Mockito.when(pnDataVaultClientReactive.getMandatesByIds(Mockito.any())).thenReturn(flux);

        Mono<BaseRecipientDtoInt> monoDelegateInfo = confidentialInformationService.getDelegateInformationByMandateId(mandateId, delegateType);

        Assertions.assertThrows(PnInternalException.class, monoDelegateInfo::block);
    }
}