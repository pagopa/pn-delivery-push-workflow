package it.pagopa.pn.deliverypushworkflow.action.it.mockbean;

import it.pagopa.pn.deliverypushworkflow.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypushworkflow.middleware.dao.documentcreationdao.DocumentCreationRequestDao;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class DocumentCreationRequestDaoMock implements DocumentCreationRequestDao {
    private ConcurrentMap<String, DocumentCreationRequest> documentMap;

    public void clear() {
        this.documentMap = new ConcurrentHashMap<>();
    }

    @Override
    public void addDocumentCreationRequest(DocumentCreationRequest documentCreationRequest) {
        if(documentMap.get(documentCreationRequest.getKey()) != null){
            log.error("[TEST] Cannot save more than one addDocumentCreationRequest with same fileKey {}",documentCreationRequest.getKey());
            throw new RuntimeException("Cannot save more than one addDocumentCreationRequest with same fileKey");
        }
        documentMap.put(documentCreationRequest.getKey(), documentCreationRequest);
        log.info("document added to documentMap {}", documentCreationRequest);
    }
}
