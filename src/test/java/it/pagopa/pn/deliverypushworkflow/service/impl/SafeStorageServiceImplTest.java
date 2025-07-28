package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.dto.ext.safestorage.FileCreationResponseInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.FileCreationResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

class SafeStorageServiceImplTest {
    @Mock
    private PnSafeStorageClient safeStorageClient;
    
    private SafeStorageServiceImpl safeStorageService;
    
    @BeforeEach
    public void init(){
        safeStorageService = new SafeStorageServiceImpl( safeStorageClient);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void createAndUploadContent() {
        //GIVEN
        FileCreationWithContentRequest fileCreationWithContentRequest = new FileCreationWithContentRequest();
        fileCreationWithContentRequest.setContent("content".getBytes());

        FileCreationResponse expectedResponse = new FileCreationResponse();
        expectedResponse.setKey("key");
        expectedResponse.setSecret("secret");
        
        Mockito.when(safeStorageClient.createFile(Mockito.any(FileCreationWithContentRequest.class), Mockito.anyString()))
                .thenReturn(Mono.just(expectedResponse));

        //WHEN
        Mono<FileCreationResponseInt> responseMono = safeStorageService.createAndUploadContent(fileCreationWithContentRequest);

        //THEN
        FileCreationResponseInt response = responseMono.block();
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getKey(), expectedResponse.getKey());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void createAndUploadContentError() {
        //GIVEN
        FileCreationWithContentRequest fileCreationWithContentRequest = new FileCreationWithContentRequest();
        fileCreationWithContentRequest.setContent("content".getBytes());
        
        Mockito.when(safeStorageClient.createFile(Mockito.any(FileCreationWithContentRequest.class), Mockito.anyString()))
                .thenReturn(Mono.error(new PnInternalException("test", "test")));

        //WHEN
        Mono<FileCreationResponseInt> mono = safeStorageService.createAndUploadContent(fileCreationWithContentRequest);

        Assertions.assertThrows( PnInternalException.class, mono::block);
    }
}