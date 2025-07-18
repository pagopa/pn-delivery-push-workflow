package it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.dynamo.mapper;

import it.pagopa.pn.deliverypushworkflow.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.dynamo.entity.PaperNotificationFailedEntity;
import org.springframework.stereotype.Component;

@Component
public class EntityToDtoNotificationFailedMapper {

    public PaperNotificationFailed entityToDto(PaperNotificationFailedEntity entity) {
        return PaperNotificationFailed.builder()
                .iun(entity.getIun())
                .recipientId(entity.getRecipientId())
                .build();
    }
}

