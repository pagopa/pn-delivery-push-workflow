package it.pagopa.pn.deliverypushworkflow.action.it.mockbean;

import it.pagopa.pn.deliverypushworkflow.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypushworkflow.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.FileCreationResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.OperationResultCodeResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class SafeStorageClientMock implements PnSafeStorageClient {
    private Map<String, FileCreationWithContentRequest> savedFileMap = new HashMap<>();

    public void clear() {
        this.savedFileMap =  new HashMap<>();
    }

    @Override
    public Mono<FileCreationResponse> createFile(FileCreationWithContentRequest fileCreationRequest, String sha256) {
        log.info("[TEST] createFile documentType={}", fileCreationRequest.getDocumentType());

        String key = "RANDOM_"+ UUID.randomUUID();
        if(savedFileMap.get(key) != null){
            log.error("[TEST] Cannot save more than one file with same fileKey {}",key);
            return Mono.error(new RuntimeException("Cannot save more than one file with same fileKey"));
        }
        
        savedFileMap.put(key,fileCreationRequest);
        
        FileCreationResponse fileCreationResponse = new FileCreationResponse();
        fileCreationResponse.setKey(key);
        fileCreationResponse.setSecret("abc");
        fileCreationResponse.setUploadUrl("https://www.unqualcheurl.it");
        fileCreationResponse.setUploadMethod(FileCreationResponse.UploadMethodEnum.POST);
        
        return Mono.just(fileCreationResponse);
    }

    @Override
    public Mono<OperationResultCodeResponse> updateFileMetadata(String fileKey, UpdateFileMetadataRequest request) {
        OperationResultCodeResponse operationResultCodeResponse = new OperationResultCodeResponse();
        operationResultCodeResponse.setResultCode("200.00");
        operationResultCodeResponse.setResultDescription("OK");
        
        return Mono.just(operationResultCodeResponse);
    }

    @Override
    public void uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponse fileCreationResponse, String sha256) {
        log.info("[TEST] Upload content Mock - key={} uploadUrl={}", fileCreationResponse.getKey(), fileCreationResponse.getUploadUrl());

    }

    public void writeFile(String fileKey, LegalFactCategoryInt legalFactCategory, String testName){
        FileCreationWithContentRequest fileCreationRequest = savedFileMap.get(fileKey);

        String ext = getExtensionFromContentType(fileCreationRequest.getContentType());
        String TEST_DIR_NAME = "target" + File.separator + "generated-test-PDF-IT";
        Path TEST_DIR_PATH = Paths.get(TEST_DIR_NAME);

        //create target test folder, if not exists
        if (Files.notExists(TEST_DIR_PATH)) {
            try {
                Files.createDirectory(TEST_DIR_PATH);
            } catch (IOException e) {
                System.out.println("Exception in uploadContent " + e);
            }
        }

        Path filePath = Paths.get(TEST_DIR_NAME + File.separator + testName+ "-"+ legalFactCategory.getValue() + "." + ext);
        try {
            Files.write(filePath, fileCreationRequest.getContent());
        } catch (IOException e) {
            System.out.println("Exception in uploadContent " + e);
        }
    }
    
    private String getExtensionFromContentType(String contentType) {
        switch (contentType){
            case "application/pdf":
                return "pdf";
            case "text/html":
                return "html";
            default:
                System.out.println("Content type not expected "+contentType);
                return "pdf";
        }
    }
}
