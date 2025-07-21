package it.pagopa.pn.deliverypushworkflow.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.v1.ApiClient;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.v1.api.FileDownloadApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.v1.api.FileMetadataUpdateApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.v1.api.FileUploadApi;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SafeStorageApiReactiveConfigurator extends CommonBaseClient {

    @Bean
    public FileUploadApi fileUploadApiReactive(PnDeliveryPushWorkflowConfigs cfg){
        return new FileUploadApi( getNewApiClient(cfg) );
    }

    @Bean
    public FileDownloadApi fileDownloadApiReactive(PnDeliveryPushWorkflowConfigs cfg){
        return new FileDownloadApi( getNewApiClient(cfg) );
    }

    @Bean
    public FileMetadataUpdateApi fileMetadataUpdateApiReactive(PnDeliveryPushWorkflowConfigs cfg){
        return new FileMetadataUpdateApi( getNewApiClient(cfg) );
    }
    
    @NotNull
    private ApiClient getNewApiClient(PnDeliveryPushWorkflowConfigs cfg) {
        ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()) );
        newApiClient.setBasePath( cfg.getSafeStorageBaseUrl() );
        return newApiClient;
    }
}
