package it.pagopa.pn.deliverypushworkflow.dto.ext.paperchannel;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@Getter
@Setter
@ToString
public class CategorizedAttachmentsResultInt {

    private List<ResultFilterInt> acceptedAttachments = null;
    private List<ResultFilterInt> discardedAttachments = null;
}
