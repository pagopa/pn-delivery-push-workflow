package it.pagopa.pn.deliverypushworkflow.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.deliverypush.ApiClient;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.deliverypush.api.NotificationProcessCostApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeliveryPushApiReactiveConfiguration extends CommonBaseClient {

    @Bean
    public NotificationProcessCostApi deliveryPushPrivateApiConfig(PnDeliveryPushWorkflowConfigs cfg) {
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(cfg.getDeliveryPushBaseUrl());
        return new NotificationProcessCostApi(apiClient);
    }

}
