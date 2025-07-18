package it.pagopa.pn.deliverypushworkflow.middleware.dao.dynamo.papernotificationfailed.mapper;

import it.pagopa.pn.deliverypushworkflow.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.dynamo.entity.PaperNotificationFailedEntity;
import it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.dynamo.mapper.DtoToEntityNotificationFailedMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

class DtoToEntityNotificationFailedMapperTest {
    private DtoToEntityNotificationFailedMapper dtoToEntityNotificationFailedMapper;

    @BeforeEach
    void instantiateDao() {
        dtoToEntityNotificationFailedMapper = new DtoToEntityNotificationFailedMapper();
    }

    @Test
    void dto2Entity() {
        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";
        String idRecipient = "paMi2";

        PaperNotificationFailed failedNot = PaperNotificationFailed.builder()
                .iun(iun)
                .recipientId(idRecipient).build();

        PaperNotificationFailedEntity entity = dtoToEntityNotificationFailedMapper.dto2Entity(failedNot);

        assertEquals(failedNot.getIun(), entity.getIun());
        assertEquals(failedNot.getRecipientId(), entity.getRecipientId());
    }
}