package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;


import it.pagopa.pn.deliverypushworkflow.dto.address.DigitalAddressSourceInt;

public interface DigitalAddressSourceRelatedTimelineElement  extends RecipientRelatedTimelineElementDetails {
    DigitalAddressSourceInt getDigitalAddressSource();
    void setDigitalAddressSource(DigitalAddressSourceInt digitalAddressInt);
}
