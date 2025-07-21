package it.pagopa.pn.deliverypushworkflow.config.msclient;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalchannel.ApiClient;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalchannel.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalchannel.api.DigitalLegalMessagesApi;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ExternalChannelApiConfigurator {
    @Bean
    @Primary
    public DigitalLegalMessagesApi digitalLegalMessagesApi(@Qualifier("withOffsetDateTimeFormatter") RestTemplate restTemplate, PnDeliveryPushWorkflowConfigs cfg){
        return new DigitalLegalMessagesApi(newApiClient(restTemplate, cfg));
    }

    @Bean
    @Primary
    public DigitalCourtesyMessagesApi digitalCourtesyMessagesApi(@Qualifier("withOffsetDateTimeFormatter") RestTemplate restTemplate, PnDeliveryPushWorkflowConfigs cfg){
        return new DigitalCourtesyMessagesApi(newApiClient(restTemplate, cfg));
    }
    
    private ApiClient newApiClient(RestTemplate restTemplate, PnDeliveryPushWorkflowConfigs cfg)
    {
        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(cfg.getExternalChannelBaseUrl());
        return apiClient;
    }
}
