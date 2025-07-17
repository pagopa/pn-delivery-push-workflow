package it.pagopa.pn.deliverypushworkflow.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.ApiClient;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.api.UpdateNotificationCostApi;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExternalRegistriesApiReactiveConfigurator extends CommonBaseClient {

    @Bean
    public UpdateNotificationCostApi updateNotificationCostApi(PnDeliveryPushWorkflowConfigs cfg){
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(cfg.getExternalRegistryBaseUrl());
        return new UpdateNotificationCostApi(apiClient);
    }
}
