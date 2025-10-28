package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypushworkflow.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.FileCreationResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.OperationResultCodeResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.UpdateFileMetadataRequest;
import reactor.core.publisher.Mono;

public interface PnSafeStorageClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE;
    String CREATE_FILE = "FILE CREATION";
    String UPDATE_FILE_METADATA = "UPDATE FILE METADATA";
    String UPLOAD_FILE_CONTENT = "UPLOAD FILE CONTENT";

    String SAFE_STORAGE_URL_PREFIX = "safestorage://";

    Mono<FileCreationResponse> createFile(FileCreationWithContentRequest fileCreationRequest, String sha256);

    Mono<OperationResultCodeResponse> updateFileMetadata(String fileKey, UpdateFileMetadataRequest request);

    void uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponse fileCreationResponse, String sha256);

    Mono<FileDownloadResponse> getFile(String fileKey, Boolean metadataOnly, Boolean tags);
}
