package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.deliverypushworkflow.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypushworkflow.service.PaperNotificationFailedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaperNotificationFailedServiceImpl implements PaperNotificationFailedService {
    private final PaperNotificationFailedDao paperNotificationFailedDao;
    
    public PaperNotificationFailedServiceImpl(PaperNotificationFailedDao paperNotificationFailedDao) {
        this.paperNotificationFailedDao = paperNotificationFailedDao;
    }

    @Override
    public void addPaperNotificationFailed(PaperNotificationFailed paperNotificationFailed) {
        paperNotificationFailedDao.addPaperNotificationFailed(paperNotificationFailed);
    }

    @Override
    public void deleteNotificationFailed(String recipientId, String iun) {
        log.info("PaperNotificationFailed delete for recipientId={} iun={}", recipientId, iun);
        paperNotificationFailedDao.deleteNotificationFailed(recipientId, iun);
    }


}
