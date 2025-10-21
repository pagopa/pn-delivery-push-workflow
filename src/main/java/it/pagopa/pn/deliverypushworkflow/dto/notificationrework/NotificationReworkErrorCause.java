package it.pagopa.pn.deliverypushworkflow.dto.notificationrework;

import lombok.Getter;

@Getter
public enum NotificationReworkErrorCause {
    INVALID_RECINDEX("INVALID_RECINDEX", "Il recIndex per il quale è stata richiesta l'invalidazione non esiste"),
    INVALID_NOTIFICATION_STATUS("INVALID_NOTIFICATION_STATUS", "La notifica è in stato %s, gli stati validi sono %s"),
    INVALID_EXPECTED_STATUS_CODE("INVALID_EXPECTED_STATUS_CODE", "Lo stato finale atteso %s non è coerente con l'attempt %s"),
    INVALID_ATTEMPT_ID("INVALID_ATTEMPT_ID", "L'attempt per il quale è stata richiesta l'invalidazione non esiste"),
    INVALID_TIMELINE_ELEMENT("INVALID_TIMELINE_ELEMENT", "%s");

    private final String cause;
    private final String errorDetails;

    NotificationReworkErrorCause(String cause, String errorDetails) {
        this.cause = cause;
        this.errorDetails = errorDetails;
    }

}
