package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt;

public interface CourtesyAddressRelatedTimelineElement extends ConfidentialInformationTimelineElement{
    CourtesyDigitalAddressInt getDigitalAddress();
    void setDigitalAddress(CourtesyDigitalAddressInt digitalAddressInt);
}
