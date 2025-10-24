package it.pagopa.pn.deliverypushworkflow.exceptions;

import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkError;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class NotificationReworkValidationException extends RuntimeException {

    private final List<NotificationReworkError> errors = new ArrayList<>();

    public NotificationReworkValidationException(NotificationReworkError error) {
        super(error.getDescription());
        this.errors.add(error);
    }
}
