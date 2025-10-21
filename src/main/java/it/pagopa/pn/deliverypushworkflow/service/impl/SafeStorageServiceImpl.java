package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypushworkflow.dto.ext.safestorage.FileCreationResponseInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypushworkflow.dto.ext.safestorage.UpdateFileMetadataResponseInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypushworkflow.service.SafeStorageService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.util.Base64;

import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.*;

@Slf4j
@Service
public class SafeStorageServiceImpl implements SafeStorageService {
    private final PnSafeStorageClient safeStorageClient;

    public SafeStorageServiceImpl(PnSafeStorageClient safeStorageClient) {
        this.safeStorageClient = safeStorageClient;
    }
    
    @Override
    public Mono<FileCreationResponseInt> createAndUploadContent(FileCreationWithContentRequest fileCreationRequest) {
        log.info("Start createAndUploadFile - documentType={} filesize={}", fileCreationRequest.getDocumentType(), fileCreationRequest.getContent().length);

        String sha256 = computeSha256(fileCreationRequest.getContent());

        return safeStorageClient.createFile(fileCreationRequest, sha256)
                .onErrorResume( Exception.class, exception ->{
                    log.error("Cannot create file ", exception);
                    return Mono.error(new PnInternalException("Cannot create file", ERROR_CODE_DELIVERYPUSH_UPLOADFILEERROR, exception));
                })
                .flatMap(fileCreationResponse -> 
                    Mono.fromRunnable(() -> safeStorageClient.uploadContent(fileCreationRequest, fileCreationResponse, sha256))
                            .thenReturn(fileCreationResponse)
                            .map(fileCreationResponse2 ->{
                                FileCreationResponseInt fileCreationResponseInt = FileCreationResponseInt.builder()
                                        .key(fileCreationResponse2.getKey())
                                        .build();

                                log.info("createAndUploadContent file uploaded successfully key={} sha256={}", fileCreationResponseInt.getKey(), sha256);

                                return fileCreationResponseInt;
                            })
                );
    }
    
    @Override
    public Mono<UpdateFileMetadataResponseInt> updateFileMetadata(String fileKey, UpdateFileMetadataRequest updateFileMetadataRequest) {
        MDC.put(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY, fileKey);
        log.debug("Start call updateFileMetadata - fileKey={} updateFileMetadataRequest={}", fileKey, updateFileMetadataRequest);

        return safeStorageClient.updateFileMetadata(fileKey, updateFileMetadataRequest)
                .doOnSuccess( res -> {
                    MDC.remove(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY);
                    log.debug("updateFileMetadata file ok key={} updateFileMetadataResponseInt={}", fileKey, updateFileMetadataRequest);
                })
                .onErrorResume( err ->{
                    log.error("Cannot update metadata ", err);
                    MDC.remove(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY);
                    return Mono.error(new PnInternalException("Cannot update metadata", ERROR_CODE_DELIVERYPUSH_UPDATEMETAFILEERROR, err));
                })
                .map( res -> UpdateFileMetadataResponseInt.builder()
                            .resultCode(res.getResultCode())
                            .errorList(res.getErrorList())
                            .resultDescription(res.getResultDescription())
                            .build()
                );
    }

    @Override
    public Mono<FileDownloadResponse> getFile(String fileKey, Boolean metadataOnly, Boolean tags) {
        return safeStorageClient.getFile( fileKey, metadataOnly, tags);
    }

    private String computeSha256( byte[] content ) {

        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest( content );
            return bytesToBase64( encodedhash );
        } catch (Exception exc) {
            throw new PnInternalException("cannot compute sha256", ERROR_CODE_DELIVERYPUSH_ERRORCOMPUTECHECKSUM, exc );
        }
    }

    private static String bytesToBase64(byte[] hash) {
        return Base64.getEncoder().encodeToString( hash );
    }
}
