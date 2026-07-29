package it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.datavault.RecipientTypeInt;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class NotificationRecipientInt {
    @ToString.Exclude
    private String taxId;
    private String internalId;
    @ToString.Exclude
    private String denomination;
    private LegalDigitalAddressInt digitalDomicile;
    @ToString.Exclude
    private PhysicalAddressInt physicalAddress;
    private List<NotificationPaymentInfoInt> payments;
    private RecipientTypeInt recipientType;
}
