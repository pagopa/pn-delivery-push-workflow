package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.dto.documentcreation.DocumentCreationTypeInt;

public interface DocumentCreationRequestService {
    void addDocumentCreationRequest(String fileKey, String iun, Integer recIndex, DocumentCreationTypeInt documentType, String timelineId);

    void addDocumentCreationRequest(String fileKey, String iun, DocumentCreationTypeInt documentType, String timelineId);
}
