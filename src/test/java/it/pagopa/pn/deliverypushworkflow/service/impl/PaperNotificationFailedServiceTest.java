package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.deliverypushworkflow.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypushworkflow.service.PaperNotificationFailedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PaperNotificationFailedServiceTest {
    private static final String IUN = "IUN";
    private static final String RECIPIENT_ID = "RECIPIENT_ID";
    private PaperNotificationFailedDao paperNotificationFailedDao;
    private PaperNotificationFailedService paperNotificationFailedService;

    @BeforeEach
    void setup() {
        paperNotificationFailedDao = Mockito.mock( PaperNotificationFailedDao.class );
        
        paperNotificationFailedService = new PaperNotificationFailedServiceImpl( paperNotificationFailedDao);
    }

    @Test
    void addPaperNotificationFailedShouldInvokeDaoWithGivenObject() {
        PaperNotificationFailed paperNotificationFailed = Mockito.mock(PaperNotificationFailed.class);

        paperNotificationFailedService.addPaperNotificationFailed(paperNotificationFailed);

        Mockito.verify(paperNotificationFailedDao).addPaperNotificationFailed(paperNotificationFailed);
    }

    @Test
    void deleteNotificationFailedShouldInvokeDaoWithCorrectParameters() {
        paperNotificationFailedService.deleteNotificationFailed(RECIPIENT_ID, IUN);

        Mockito.verify(paperNotificationFailedDao).deleteNotificationFailed(RECIPIENT_ID, IUN);
    }
}
