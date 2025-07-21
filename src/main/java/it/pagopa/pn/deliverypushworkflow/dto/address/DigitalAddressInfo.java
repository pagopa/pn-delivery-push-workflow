package it.pagopa.pn.deliverypushworkflow.dto.address;

import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@SuperBuilder( toBuilder = true )
@EqualsAndHashCode
@ToString
public class DigitalAddressInfo {
    private LegalDigitalAddressInt digitalAddress;
    private DigitalAddressSourceInt digitalAddressSource;
}
