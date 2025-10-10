package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.datavault.RecipientTypeInt;
import reactor.core.publisher.Mono;

public interface ConfidentialInformationService {
    Mono<BaseRecipientDtoInt> getRecipientInformationByInternalId(String internalId);

    Mono<BaseRecipientDtoInt> getDelegateInformationByMandateId(String mandateId, RecipientTypeInt delegateType);
}
