package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;


import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;

public interface DigitalAddressRelatedTimelineElement extends ConfidentialInformationTimelineElement{
    LegalDigitalAddressInt getDigitalAddress();
    void setDigitalAddress(LegalDigitalAddressInt digitalAddressInt);
}
