package it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao;



import it.pagopa.pn.deliverypushworkflow.dto.papernotificationfailed.PaperNotificationFailed;

public interface PaperNotificationFailedDao {
    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.failed-notification";

    void addPaperNotificationFailed(PaperNotificationFailed paperNotificationFailed);

    void deleteNotificationFailed(String recipientId, String iun);

}
