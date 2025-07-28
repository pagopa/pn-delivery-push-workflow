package it.pagopa.pn.deliverypushworkflow.middleware.dao.documentcreationdao;

import it.pagopa.pn.deliverypushworkflow.dto.documentcreation.DocumentCreationRequest;

public interface DocumentCreationRequestDao {
    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.document-creation";
    
    void addDocumentCreationRequest(DocumentCreationRequest documentCreationRequest);
}
