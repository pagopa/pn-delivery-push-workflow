package it.pagopa.pn.deliverypushworkflow.middleware.dao.documentcreationdao.dynamo.mapper;


import it.pagopa.pn.deliverypushworkflow.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypushworkflow.middleware.dao.documentcreationdao.dynamo.entity.DocumentCreationRequestEntity;
import org.springframework.stereotype.Component;

@Component
public class DtoToEntityDocumentCreationRequestMapper {
    
    public DocumentCreationRequestEntity dto2Entity(DocumentCreationRequest dto) {
        return DocumentCreationRequestEntity.builder()
                .key(dto.getKey())
                .iun(dto.getIun())
                .recIndex(dto.getRecIndex())
                .documentType(dto.getDocumentCreationType().getValue())
                .timelineId(dto.getTimelineId())
                .build();
    }
}
