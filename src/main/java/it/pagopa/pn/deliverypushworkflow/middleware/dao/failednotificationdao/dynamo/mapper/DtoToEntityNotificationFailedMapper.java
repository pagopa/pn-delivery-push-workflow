package it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.dynamo.mapper;

import it.pagopa.pn.deliverypushworkflow.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.dynamo.entity.PaperNotificationFailedEntity;
import org.springframework.stereotype.Component;

@Component
public class DtoToEntityNotificationFailedMapper {
    
    public PaperNotificationFailedEntity dto2Entity(PaperNotificationFailed dto) {
        return PaperNotificationFailedEntity.builder()
                .recipientId(dto.getRecipientId())
                .iun(dto.getIun())
                .build();
    }
}
