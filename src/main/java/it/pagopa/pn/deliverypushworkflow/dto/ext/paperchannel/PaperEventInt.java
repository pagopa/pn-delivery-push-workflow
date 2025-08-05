package it.pagopa.pn.deliverypushworkflow.dto.ext.paperchannel;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class PaperEventInt {
    private String requestId;
    private String iun;
    private String statusCode;
    private Instant statusDateTime;
    private String statusDetail;


}
