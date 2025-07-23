package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import it.pagopa.pn.deliverypushworkflow.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.utils.AuditLogUtils;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class DigitalDeliveryCreationRequestDetailsInt implements RecipientRelatedTimelineElementDetails, DigitalAddressRelatedTimelineElement{
    private int recIndex;
    private EndWorkflowStatus endWorkflowStatus;
    private Instant completionWorkflowDate;
    private LegalDigitalAddressInt digitalAddress;
    private String legalFactId;

    public String toLog() {
        return String.format(
                "recIndex=%d endWorkflowStatus%s completionWorkflowDate=%s digitalAddress=%s legalFactId=%s",
                recIndex,
                endWorkflowStatus,
                completionWorkflowDate,
                AuditLogUtils.SENSITIVE,
                legalFactId
        );
    }
}