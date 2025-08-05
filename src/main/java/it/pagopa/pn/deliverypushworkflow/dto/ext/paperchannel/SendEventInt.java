package it.pagopa.pn.deliverypushworkflow.dto.ext.paperchannel;

import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.externalchannel.AttachmentDetailsInt;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString
public class SendEventInt extends PaperEventInt {

    private String statusDescription;
    private List<AttachmentDetailsInt> attachments = null;
    private PhysicalAddressInt discoveredAddress;
    private String deliveryFailureCause;

    private String registeredLetterCode;

}
