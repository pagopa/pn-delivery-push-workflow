package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@ToString
public class PrepareAnalogDomicileFailureDetailsInt extends CategoryTypeTimelineElementDetailsInt implements RecipientRelatedTimelineElementDetails, PhysicalAddressRelatedTimelineElement {

    private int recIndex;
    private PhysicalAddressInt foundAddress;
    private String failureCause;
    private String prepareRequestId;

    public String toLog() {
        return String.format(
            "recIndex=%d failureCause=%s prepareRequestId=%s",
            recIndex,
            failureCause,
            prepareRequestId
        );
    }

    @Override
    public PhysicalAddressInt getPhysicalAddress() {
        return foundAddress;
    }

    @Override
    public void setPhysicalAddress(PhysicalAddressInt physicalAddressInt) {
        this.foundAddress = physicalAddressInt;
    }
}