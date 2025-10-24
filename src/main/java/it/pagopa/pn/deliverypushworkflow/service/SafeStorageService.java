package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.dto.ext.safestorage.FileCreationResponseInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypushworkflow.dto.ext.safestorage.UpdateFileMetadataResponseInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.UpdateFileMetadataRequest;
import reactor.core.publisher.Mono;

public interface SafeStorageService {
    
    Mono<FileCreationResponseInt> createAndUploadContent(FileCreationWithContentRequest fileCreationRequest);

    Mono<UpdateFileMetadataResponseInt> updateFileMetadata(String fileKey, UpdateFileMetadataRequest updateFileMetadataRequest);

    Mono<FileDownloadResponse> getFile(String fileKey, Boolean metadataOnly, Boolean tags);
}
