package it.pagopa.pn.deliverypushworkflow.dto.ext.paperchannel;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.model.ResultFilterEnum;
import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@Getter
@Setter
@ToString
public class ResultFilterInt {

    private String fileKey;
    private ResultFilterEnum result;
    private String reasonCode;
    private String reasonDescription;
}
