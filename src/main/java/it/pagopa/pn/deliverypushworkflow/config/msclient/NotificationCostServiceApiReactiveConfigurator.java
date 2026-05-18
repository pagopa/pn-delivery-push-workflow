package it.pagopa.pn.deliverypushworkflow.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.notificationcostservice_reactive.ApiClient;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.notificationcostservice_reactive.api.PaperCostApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationCostServiceApiReactiveConfigurator extends CommonBaseClient {

    @Bean
    public PaperCostApi notificationCostPaperCostApi(PnDeliveryPushWorkflowConfigs cfg) {
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        String baseUrl = cfg.getNotificationCostServiceBaseUrl() != null
                ? cfg.getNotificationCostServiceBaseUrl()
                : cfg.getExternalRegistryBaseUrl();
        apiClient.setBasePath(baseUrl);
        return new PaperCostApi(apiClient);
    }
}

