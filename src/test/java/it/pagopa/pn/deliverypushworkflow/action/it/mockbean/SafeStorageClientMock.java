package it.pagopa.pn.deliverypushworkflow.action.it.mockbean;

import it.pagopa.pn.deliverypushworkflow.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.MethodExecutor;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypushworkflow.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypushworkflow.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypushworkflow.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.FileCreationResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.OperationResultCodeResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypushworkflow.service.SchedulerService;
import it.pagopa.pn.deliverypushworkflow.service.utils.FileUtils;
import it.pagopa.pn.deliverypushworkflow.utils.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class SafeStorageClientMock implements PnSafeStorageClient {
    private Map<String, FileCreationWithContentRequest> savedFileMap = new HashMap<>();
    private final SchedulerService schedulerService;
    private final DocumentCreationRequestDaoMock documentCreationRequestDaoMock;

    public SafeStorageClientMock(DocumentCreationRequestDaoMock documentCreationRequestDaoMock, SchedulerService schedulerService) {
        this.documentCreationRequestDaoMock = documentCreationRequestDaoMock;
        this.schedulerService = schedulerService;
    }

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

        ThreadPool.start( new Thread(() -> Assertions.assertDoesNotThrow(() -> {
            String keyWithPrefix = FileUtils.getKeyWithStoragePrefix(key);

            log.info("[TEST] Start wait for createFile documentType={} keyWithPrefix={}",fileCreationRequest.getDocumentType(), keyWithPrefix);

            if(! TestUtils.PN_NOTIFICATION_ATTACHMENT.equals(fileCreationRequest.getDocumentType())){
                log.info("[TEST] Start wait for createFile in IF documentType={} keyWithPrefix={}",fileCreationRequest.getDocumentType(), keyWithPrefix);

                MethodExecutor.waitForExecution(
                        () -> documentCreationRequestDaoMock.getDocumentCreationRequest(keyWithPrefix)
                );
                log.info("[TEST] Ended waitForExecution");

                Optional<DocumentCreationRequest> documentCreationRequestOptional = documentCreationRequestDaoMock.getDocumentCreationRequest(keyWithPrefix);
                if (documentCreationRequestOptional.isPresent()) {
                    DocumentCreationRequest documentCreationRequest = documentCreationRequestOptional.get();
                    scheduleHandleDocumentCreationResponse(documentCreationRequest);
                } else {
                    log.warn("[TEST] DocumentCreationRequest non trovato per keyWithPrefix={}", keyWithPrefix);
                }

                log.info("[TEST] END wait for createFile documentType={} keyWithPrefix={}",fileCreationRequest.getDocumentType(), keyWithPrefix);

            } else{
                log.info("[TEST] No need to wait response for PN_NOTIFICATION_ATTACHMENT");
            }
        })));
        
        FileCreationResponse fileCreationResponse = new FileCreationResponse();
        fileCreationResponse.setKey(key);
        fileCreationResponse.setSecret("abc");
        fileCreationResponse.setUploadUrl("https://www.unqualcheurl.it");
        fileCreationResponse.setUploadMethod(FileCreationResponse.UploadMethodEnum.POST);
        
        return Mono.just(fileCreationResponse);
    }

    private void scheduleHandleDocumentCreationResponse(DocumentCreationRequest request) {
        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
                .documentCreationType(request.getDocumentCreationType())
                .key(request.getKey())
                .timelineId(request.getTimelineId())
                .build();

        Instant schedulingDate = Instant.now();

        //Effettuando lo scheduling dell'evento siamo sicuri che l'evento verrà gestito una sola volta, dal momento che lo scheduling è in  putIfAbsent
        log.info("Scheduling HandleDocumentCreationResponse schedulingDate={} - iun={} recIndex={} docType={}", schedulingDate, request.getIun(), request.getRecIndex(), request.getDocumentCreationType());
        schedulerService.scheduleEvent(request.getIun(), request.getRecIndex(), schedulingDate, ActionType.DOCUMENT_CREATION_RESPONSE, request.getTimelineId(), details);
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
        String testDirName = "target" + File.separator + "generated-test-PDF-IT";
        Path testDirPath = Paths.get(testDirName);

        //create target test folder, if not exists
        if (Files.notExists(testDirPath)) {
            try {
                Files.createDirectory(testDirPath);
            } catch (IOException e) {
                System.out.println("Exception in uploadContent " + e);
            }
        }

        Path filePath = Paths.get(testDirName + File.separator + testName+ "-"+ legalFactCategory.getValue() + "." + ext);
        try {
            Files.write(filePath, fileCreationRequest.getContent());
        } catch (IOException e) {
            System.out.println("Exception in uploadContent " + e);
        }
    }
    
    private String getExtensionFromContentType(String contentType) {
        return switch (contentType) {
            case "application/pdf" -> "pdf";
            case "text/html" -> "html";
            default -> {
                System.out.println("Content type not expected " + contentType);
                yield "pdf";
            }
        };
    }
}
