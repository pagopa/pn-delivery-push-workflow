package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.FileCreationRequest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.FileCreationResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.OperationResultCodeResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.v1.api.FileMetadataUpdateApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.v1.api.FileUploadApi;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
@CustomLog
public class PnSafeStorageClientImpl extends CommonBaseClient implements PnSafeStorageClient {
    private final FileUploadApi fileUploadApi;
    private final FileMetadataUpdateApi fileMetadataUpdateApi;
    private final RestTemplate restTemplate;
    private final PnDeliveryPushWorkflowConfigs cfg;

    public PnSafeStorageClientImpl(PnDeliveryPushWorkflowConfigs cfg,
                                   @Qualifier("withOffsetDateTimeFormatter") RestTemplate restTemplate,
                                   FileUploadApi fileUploadApi,
                                   FileMetadataUpdateApi fileMetadataUpdateApi) {
        this.cfg = cfg;
        this.fileUploadApi = fileUploadApi;
        this.fileMetadataUpdateApi = fileMetadataUpdateApi;
        this.restTemplate = restTemplate;
    }

    @Override
    public Mono<FileCreationResponse> createFile(FileCreationWithContentRequest fileCreationRequestWithContent, String sha256) {
        log.logInvokingExternalService(CLIENT_NAME, CREATE_FILE);

        FileCreationRequest fileCreationRequest = new FileCreationRequest();
        fileCreationRequest.setContentType(fileCreationRequestWithContent.getContentType());
        fileCreationRequest.setDocumentType(fileCreationRequestWithContent.getDocumentType());
        fileCreationRequest.setStatus(fileCreationRequestWithContent.getStatus());
        fileCreationRequest.setChecksumValue(sha256);

        return fileUploadApi.createFile( this.cfg.getSafeStorageCxId(), fileCreationRequest )
                .doOnError( res -> log.error("File creation error - documentType={} filesize={} sha256={}", fileCreationRequest.getDocumentType(), fileCreationRequestWithContent.getContent().length, fileCreationRequest.getChecksumValue()));
    }

    @Override
    @Retryable(
            value = {PnInternalException.class},
            maxAttempts = 3,
            backoff = @Backoff(random = true, delay = 500, maxDelay = 1000, multiplier = 2)
    )
    public Mono<OperationResultCodeResponse> updateFileMetadata(String fileKey, UpdateFileMetadataRequest request) {
        log.logInvokingExternalService(CLIENT_NAME, UPDATE_FILE_METADATA);

        return fileMetadataUpdateApi.updateFileMetadata( fileKey, this.cfg.getSafeStorageCxIdUpdatemetadata(), request )
                .onErrorResume( err -> {
                    log.error("Exception invoking updateFileMetadata fileKey={} err ",fileKey, err);
                    return Mono.error(new PnInternalException("Exception invoking updateFileMetadata", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_UPDATEMETAFILEERROR, err));
                });
    }

    @Override
    public void uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponse fileCreationResponse, String sha256) {
        try {
            log.logInvokingAsyncExternalService(CLIENT_NAME, UPLOAD_FILE_CONTENT, fileCreationResponse.getKey());

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Content-type", fileCreationRequest.getContentType());
            headers.add("x-amz-checksum-sha256", sha256);
            headers.add("x-amz-meta-secret", fileCreationResponse.getSecret());

            HttpEntity<Resource> req = new HttpEntity<>(new ByteArrayResource(fileCreationRequest.getContent()), headers);

            URI url = URI.create(fileCreationResponse.getUploadUrl());
            HttpMethod method = fileCreationResponse.getUploadMethod() == FileCreationResponse.UploadMethodEnum.POST ? HttpMethod.POST : HttpMethod.PUT;

            ResponseEntity<String> res = restTemplate.exchange(url, method, req, String.class);

            if (res.getStatusCodeValue() != HttpStatus.OK.value())
            {
                throw new PnInternalException("File upload failed", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_UPLOADFILEERROR);
            }

        } catch (PnInternalException ee)
        {
            log.error("uploadContent PnInternalException uploading file", ee);
            throw ee;
        }
        catch (Exception ee)
        {
            log.error("uploadContent Exception uploading file", ee);
            throw new PnInternalException("Exception uploading file", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_UPLOADFILEERROR, ee);
        }
    }

}
