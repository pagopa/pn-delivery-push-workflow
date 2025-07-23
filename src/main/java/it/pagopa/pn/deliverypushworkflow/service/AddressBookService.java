package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;

import java.util.List;
import java.util.Optional;

public interface AddressBookService {
    Optional<LegalDigitalAddressInt> getPlatformAddresses(String internalId, String senderId);

    List<CourtesyDigitalAddressInt> getCourtesyAddress(String recipientId, String senderId);
}
