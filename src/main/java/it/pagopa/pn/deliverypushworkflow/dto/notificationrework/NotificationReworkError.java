package it.pagopa.pn.deliverypushworkflow.dto.notificationrework;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode
public class NotificationReworkError {
    private String cause;
    private String description;
}
