package it.pagopa.pn.deliverypushworkflow.exceptions;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkError;

import java.util.ArrayList;
import java.util.List;

public class NotificationReworkValidationException extends PnInternalException {

    private final List<NotificationReworkError> errors = new ArrayList<>();

    public NotificationReworkValidationException(NotificationReworkError error) {
        super(error.getDescription(), error.getCause());
        this.errors.add(error);
    }
}
