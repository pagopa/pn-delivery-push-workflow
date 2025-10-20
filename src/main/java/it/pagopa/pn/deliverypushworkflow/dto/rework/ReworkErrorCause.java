package it.pagopa.pn.deliverypushworkflow.dto.rework;

import lombok.Getter;

@Getter
public enum ReworkErrorCause {
    INVALID_RECINDEX("INVALID_RECINDEX", "Il recIndex per il quale è stata richiesta l'invalidazione non esiste"),
    INVALID_NOTIFICATION_STATUS("INVALID_NOTIFICATION_STATUS", "La notifica è in stato %s, gli stati validi sono %s");

    private final String cause;
    private final String errorDetails;

    ReworkErrorCause(String cause, String errorDetails) {
        this.cause = cause;
        this.errorDetails = errorDetails;
    }

}
