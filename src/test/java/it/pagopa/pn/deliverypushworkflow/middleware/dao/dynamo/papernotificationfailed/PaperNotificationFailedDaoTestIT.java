package it.pagopa.pn.deliverypushworkflow.middleware.dao.dynamo.papernotificationfailed;


import it.pagopa.pn.deliverypushworkflow.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypushworkflow.middleware.LocalStackTestConfig;
import it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@Import(LocalStackTestConfig.class)
@Disabled //per problema timeout su codebuild, ma in locale funziona
class PaperNotificationFailedDaoTestIT {

    @Autowired
    private PaperNotificationFailedDao paperNotificationFailedDao;


    @Test
    void addPaperNotificationFailedTest() {
        String iun = UUID.randomUUID().toString();
        String recipientId = "a-recipientId";
        PaperNotificationFailed dto = buildPaperNotificationFailed(recipientId, iun);
        assertDoesNotThrow(() -> paperNotificationFailedDao.addPaperNotificationFailed(dto));
    }

    @Test
    void deleteNotificationFailedWithEmptyResult() {
        String recipientId = "c-recipient";
        String iun = "iun-not-exists";

        assertDoesNotThrow(() -> paperNotificationFailedDao.deleteNotificationFailed(recipientId, iun));
    }

    private PaperNotificationFailed buildPaperNotificationFailed(String recipientId, String iun) {
        return PaperNotificationFailed.builder()
                .recipientId(recipientId)
                .iun(iun)
                .build();
    }

}