package it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class F24Int {
    private String title;
    private Boolean applyCost;
    private NotificationDocumentInt metadataAttachment;
}
