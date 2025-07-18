package it.pagopa.pn.deliverypushworkflow.dto.papernotificationfailed;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class PaperNotificationFailed {
    private String recipientId;
    private String iun;
}
