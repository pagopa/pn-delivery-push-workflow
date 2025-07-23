package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.dto.ext.datavault.BaseRecipientDtoInt;
import reactor.core.publisher.Mono;

public interface ConfidentialInformationService {
    Mono<BaseRecipientDtoInt> getRecipientInformationByInternalId(String internalId);
}
