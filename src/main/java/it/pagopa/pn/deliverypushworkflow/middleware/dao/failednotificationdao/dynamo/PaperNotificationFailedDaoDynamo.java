package it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypushworkflow.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.PaperNotificationFailedEntityDao;
import it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.dynamo.entity.PaperNotificationFailedEntity;
import it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.dynamo.mapper.DtoToEntityNotificationFailedMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Component
@ConditionalOnProperty(name = PaperNotificationFailedDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
@Slf4j
public class PaperNotificationFailedDaoDynamo implements PaperNotificationFailedDao{

    private final PaperNotificationFailedEntityDao dao;
    private final DtoToEntityNotificationFailedMapper dtoToEntity;

    public PaperNotificationFailedDaoDynamo(PaperNotificationFailedEntityDao dao,
                                            DtoToEntityNotificationFailedMapper dtoToEntity) {
        this.dao = dao;
        this.dtoToEntity = dtoToEntity;
    }

    @Override
    public void addPaperNotificationFailed(PaperNotificationFailed paperNotificationFailed) {
        PaperNotificationFailedEntity entity = dtoToEntity.dto2Entity(paperNotificationFailed);
        dao.put(entity);
    }

    @Override
    public void deleteNotificationFailed(String recipientId, String iun) {
        Key key = Key.builder()
                .partitionValue(recipientId)
                .sortValue(iun)
                .build();
        dao.delete(key);
    }
}

