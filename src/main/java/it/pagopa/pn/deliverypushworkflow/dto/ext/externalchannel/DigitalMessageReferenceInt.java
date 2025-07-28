package it.pagopa.pn.deliverypushworkflow.dto.ext.externalchannel;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class DigitalMessageReferenceInt {
    private String system;
    private String id;
    private String location;
}
