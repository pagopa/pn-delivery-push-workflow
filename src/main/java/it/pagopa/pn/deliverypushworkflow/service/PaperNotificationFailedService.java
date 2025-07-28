package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.dto.papernotificationfailed.PaperNotificationFailed;

public interface PaperNotificationFailedService {
    void addPaperNotificationFailed(PaperNotificationFailed paperNotificationFailed);
    
    void deleteNotificationFailed(String recipientId, String iun);
}
