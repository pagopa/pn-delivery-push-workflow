package it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class UsedServicesInt {
    Boolean physicalAddressLookUp;
}
