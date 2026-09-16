package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.datavault.RecipientTypeInt;
import reactor.core.publisher.Mono;

public interface ConfidentialInformationService {
    Mono<BaseRecipientDtoInt> getRecipientInformationByInternalId(String internalId);

    Mono<BaseRecipientDtoInt> getDelegateInformationByMandateId(String mandateId, RecipientTypeInt delegateType);

    Mono<String> savePlannedCourtesyAddress(String internalId, String iun, int recIndex,
                                            CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT channel, String address);

    Mono<String> getPlannedCourtesyAddress(String internalId, String plannedAddressId);
}
