package it.pagopa.pn.deliverypushworkflow.dto.notificationrework;

import lombok.Getter;

@Getter
public enum NotificationReworkErrorCause {
    NOTIFICATION_CANCELLED("NOTIFICATION_CANCELLED","La notifica è stata cancellata"),
    INVALID_RECINDEX("INVALID_RECINDEX", "Il recIndex per il quale è stata richiesta l'invalidazione non esiste"),
    INVALID_NOTIFICATION_STATUS("INVALID_NOTIFICATION_STATUS", "La notifica è in stato %s, gli stati validi sono %s"),
    EXPIRED_ATTACHMENT("EXPIRED_ATTACHMENT", "l'allegato non è più disponibile."),
    INVALID_ATTACHMENT("INVALID_ATTACHMENT", "l'allegato %s scadenza: %s."),
    INVALID_EXPECTED_STATUS_CODE("INVALID_EXPECTED_STATUS_CODE", "Non è possibile correggere l'ATTEMPT_0 di una notifica con un KO se l'ATTEMPT_1 è già presente"),
    INVALID_ATTEMPT_ID("INVALID_ATTEMPT_ID", "L'attempt per il quale è stata richiesta l'invalidazione non esiste"),
    INVALID_TIMELINE_ELEMENT("INVALID_TIMELINE_ELEMENT", "%s"),
    INVALID_ANALOG_ADDRESS("INVALID_ANALOG_ADDRESS", "L'indirizzo trovato ma scade nel %s"),
    EXPIRED_ANALOG_ADDRESS("EXPIRED_ANALOG_ADDRESS", "Indirizzo non trovato"),
    DUPLICATED_ACTION_ERROR("DUPLICATED_ACTION_ERROR", "Errore inserimento azione duplicata"),
    REWORK_REQUESTED_PHASE_ERROR("REWORK_REQUESTED_PHASE_ERROR", "Errore durante la fase di inizializzazione della richiesta di rework"),
    INVALID_ELEMENT_CATEGORY("INVALID_ELEMENT_CATEGORY", "TimelineCateogry non valida per l'elemento: %s"),
    INVALID_CATEGORY_TO_INVALIDATE("INVALID_CATEGORY_TO_INVALIDATE", "Non è possibile invalidare un elemento %s, la category non è invalidabile"),
    INVALID_REC_INDEX("INVALID_REC_INDEX", "Il recIndex per il quale è stata richiesta l'invalidazione timeline non corrisponde a quello dell'elemento di timeline da invalidare"),
    INVALID_ANALOG_WORKFLOW_ELEMENT("INVALID_ANALOG_WORKFLOW_ELEMENT", "In caso di invalidazione di %s deve essere presente un ulteriore elemento non invalidato di SUCCESS o FAILURE per il workflow analogico"),
    INVALID_ATTEMPT1_ELEMENT("INVALID_ATTEMPT1_ELEMENT", "In caso di invalidazione di un elemento dell'attempt 1, l'attempt 0 deve essere in OK"),
    INVALID_ATTEMPT0_ELEMENT("INVALID_ATTEMPT0_ELEMENT", "Non è possibile invalidare la category %s dell'attempt 0"),
    INVALID_ATTEMPT1_ELEMENTS("INVALID_ATTEMPT1_ELEMENTS", "In caso di invalidazione di un elemento dell'attempt 1, tutti gli elementi dell'attempt 1 devono essere invalidati"),
    INVALID_VIEWED_ELEMENT("INVALID_VIEWED_ELEMENT", "In caso di invalidazione di un elemento di visualizzazione, devono essere invalidati tutti gli elementi di visualizzazione"),
    INVALID_ELEMENT_TO_INVALIDATE("INVALID_ELEMENT_TO_INVALIDATE", "Non è possibile invalidare l'elemento %s della timeline"),
    ATTACHMENTS_EXIST_ONVIEWED("ATTACHMENTS_EXIST_ONVIEWED", "La visualizzazione è stata effettuata prima della scadenza degli allegati, non è possibile procedere con la richiesta di restart"),
    INVALID_ELEMENT_TO_INVALIDATE_ATTACHMENTS_EXIST_ONVIEWED("ATTACHMENTS_EXIST_ONVIEWED", "La visualizzazione è stata effettuata prima della scadenza degli allegati, non è possibile invalidare gli elementi di visualizzazione");

    private final String cause;
    private final String errorDetails;

    NotificationReworkErrorCause(String cause, String errorDetails) {
        this.cause = cause;
        this.errorDetails = errorDetails;
    }

}
