package it.pagopa.pn.deliverypushworkflow.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.actionmanager.ApiClient;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.actionmanager.api.ActionApi;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ActionManagerApiConfigurator extends CommonBaseClient {

    @Bean
    @Primary
    public ActionApi actionApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryPushWorkflowConfigs cfg) {
        return new ActionApi(getNewApiClient(restTemplate, cfg));
    }

    @NotNull
    private ApiClient getNewApiClient( RestTemplate restTemplate, PnDeliveryPushWorkflowConfigs cfg) {
        ApiClient newApiClient = new ApiClient(restTemplate);
        newApiClient.setBasePath(cfg.getActionManagerBaseUrl());
        return newApiClient;
    }
}
