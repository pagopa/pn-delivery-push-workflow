package it.pagopa.pn.deliverypushworkflow.dto.rework;

import lombok.Getter;

@Getter
public enum NotificationReworkErrorCause {
    INVALID_RECINDEX("INVALID_RECINDEX", "Il recIndex per il quale è stata richiesta l'invalidazione non esiste"),
    INVALID_NOTIFICATION_STATUS("INVALID_NOTIFICATION_STATUS", "La notifica è in stato %s, gli stati validi sono %s"),
    EXPIRED_ATTACHMENT("EXPIRED_ATTACHMENT", "l'allegato non è più disponibile."),
    INVALID_ATTACHMENT("INVALID_ATTACHMENT", "l'allegato %s scadenza: %s.");

    private final String cause;
    private final String errorDetails;

    NotificationReworkErrorCause(String cause, String errorDetails) {
        this.cause = cause;
        this.errorDetails = errorDetails;
    }

}
