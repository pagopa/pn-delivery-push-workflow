package it.pagopa.pn.deliverypushworkflow.middleware.dao.dynamo.papernotificationfailed;

import it.pagopa.pn.deliverypushworkflow.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.PaperNotificationFailedEntityDao;
import it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.dynamo.PaperNotificationFailedDaoDynamo;
import it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.dynamo.entity.PaperNotificationFailedEntity;
import it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.dynamo.mapper.DtoToEntityNotificationFailedMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.Key;

class PaperNotificationFailedDaoDynamoTest {

    @Mock
    private PaperNotificationFailedEntityDao dao;

    @Mock
    private DtoToEntityNotificationFailedMapper dtoToEntity;

    private PaperNotificationFailedDaoDynamo dynamo;

    @BeforeEach
    void setUp() {
        dao = Mockito.mock(PaperNotificationFailedEntityDao.class);
        dtoToEntity = Mockito.mock(DtoToEntityNotificationFailedMapper.class);
        dynamo = new PaperNotificationFailedDaoDynamo(dao, dtoToEntity);
    }

    @Test
    void addPaperNotificationFailed() {
        PaperNotificationFailed dto = buildPaperNotificationFailed();
        PaperNotificationFailedEntity entity = buildPaperNotificationFailedEntity();

        Mockito.when(dtoToEntity.dto2Entity(dto)).thenReturn(entity);

        dynamo.addPaperNotificationFailed(dto);

        Mockito.verify(dao, Mockito.times(1)).put(entity);
    }

    @Test
    void deleteNotificationFailed() {
        Key key = Key.builder()
                .partitionValue("001")
                .sortValue("002")
                .build();

        dynamo.deleteNotificationFailed("001", "002");

        Mockito.verify(dao, Mockito.times(1)).delete(key);
    }

    private PaperNotificationFailed buildPaperNotificationFailed() {
        return PaperNotificationFailed.builder()
                .recipientId("001")
                .iun("002")
                .build();
    }

    private PaperNotificationFailedEntity buildPaperNotificationFailedEntity() {
        return PaperNotificationFailedEntity.builder()
                .recipientId("001")
                .iun("002")
                .build();
    }
}