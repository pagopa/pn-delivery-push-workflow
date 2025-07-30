package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

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
public class NotificationPaidDetailsInt extends CategoryTypeTimelineElementDetailsInt implements RecipientRelatedTimelineElementDetails, ElementTimestampTimelineElementDetails {
    private int recIndex;
    private String recipientType;
    private Integer amount;
    private String creditorTaxId;
    private String noticeCode;
    private String paymentSourceChannel;
    private boolean uncertainPaymentDate;
    private Instant eventTimestamp;

    @Override
    public String toLog() {
        return this.toString(); // non ha info sensibili
    }

    @Override
    public Instant getElementTimestamp() {
        return eventTimestamp;
    }
}
