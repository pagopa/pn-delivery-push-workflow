package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;

public interface PhysicalAddressRelatedTimelineElement extends ConfidentialInformationTimelineElement{
    PhysicalAddressInt getPhysicalAddress();
    void setPhysicalAddress(PhysicalAddressInt physicalAddressInt);
}
