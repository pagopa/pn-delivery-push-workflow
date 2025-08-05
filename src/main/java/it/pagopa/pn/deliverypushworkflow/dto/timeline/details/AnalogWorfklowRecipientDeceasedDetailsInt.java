package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.utils.AuditLogUtils;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString
public class AnalogWorfklowRecipientDeceasedDetailsInt extends CategoryTypeTimelineElementDetailsInt implements RecipientRelatedTimelineElementDetails, PhysicalAddressRelatedTimelineElement, ElementTimestampTimelineElementDetails {
    private int recIndex;
    private PhysicalAddressInt physicalAddress;
    private Integer notificationCost;
    private Instant notificationDate;

    @Override
    public String toLog() {
        return String.format(
                "recIndex=%d notificationCost=%d notificationDate=%s physicalAddress=%s",
                recIndex,
                notificationCost,
                notificationDate,
                AuditLogUtils.SENSITIVE
        );
    }

    @Override
    public Instant getElementTimestamp() {
        return notificationDate;
    }

    @Override
    public void setPhysicalAddress(PhysicalAddressInt physicalAddressInt) {
        this.physicalAddress = physicalAddressInt;
    }
}