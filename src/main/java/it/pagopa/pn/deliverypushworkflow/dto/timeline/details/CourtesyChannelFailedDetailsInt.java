package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt;
import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString
public class CourtesyChannelFailedDetailsInt extends CategoryTypeTimelineElementDetailsInt implements TimelineElementDetailsInt {
    private CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT channelType;
    private DeliveryModeInt deliveryMode;
    private CourtesyChannelFailureReasonInt failureReason;

    public String toLog() {
        return String.format("channelType=%s deliveryMode=%s failureReason=%s", channelType, deliveryMode, failureReason);
    }
}
