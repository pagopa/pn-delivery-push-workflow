package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.MandateDto;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.datavault.PnDataVaultClientReactive;
import it.pagopa.pn.deliverypushworkflow.service.ConfidentialInformationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
public class ConfidentialInformationServiceImpl implements ConfidentialInformationService {
    private final PnDataVaultClientReactive pnDataVaultClientReactive;
    
    public ConfidentialInformationServiceImpl(PnDataVaultClientReactive pnDataVaultClientReactive) {
        this.pnDataVaultClientReactive = pnDataVaultClientReactive;
    }


    @Override
    public Mono<BaseRecipientDtoInt> getRecipientInformationByInternalId(String internalId) {
        return pnDataVaultClientReactive.getRecipientsDenominationByInternalId(List.of(internalId))
                .filter( el -> internalId.equals(el.getInternalId()))
                .map( el -> BaseRecipientDtoInt.builder()
                        .taxId(el.getTaxId())
                        .denomination(el.getDenomination())
                        .internalId(el.getInternalId())
                        .recipientType(el.getRecipientType() != null ? RecipientTypeInt.valueOf(el.getRecipientType().getValue()) : null)
                        .internalId(el.getInternalId())
                        .build()
                ).collectList()
                .map(list -> {
                    if(list != null && !list.isEmpty())
                        return list.get(0);
                    else 
                        return null;
                });
    }

    public Mono<BaseRecipientDtoInt> getDelegateInformationByMandateId(String mandateId, RecipientTypeInt delegateType) {
        return pnDataVaultClientReactive.getMandatesByIds(List.of(mandateId))
                .filter( el -> mandateId.equals(el.getMandateId()))
                .map( el -> BaseRecipientDtoInt.builder()
                        .denomination(buildDenominationByMandateInfo(el, delegateType))
                        .build()
                ).collectList()
                .flatMap(list -> {
                    if(list != null && !list.isEmpty())
                        return Mono.just(list.getFirst());
                    else
                        return Mono.error(new PnInternalException("Mandate not found for mandateId " + mandateId, PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_DATAVAULTMANDATES_NOT_FOUND));
                });
    }

    private String buildDenominationByMandateInfo(MandateDto el, RecipientTypeInt delegateType) {
        assert el.getInfo() != null;
        if(delegateType == RecipientTypeInt.PF) {
            return el.getInfo().getDestName() + " " + el.getInfo().getDestSurname();
        } else {
            return el.getInfo().getDestBusinessName();
        }
    }

}
