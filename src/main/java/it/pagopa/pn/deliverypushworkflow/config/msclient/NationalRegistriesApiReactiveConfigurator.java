package it.pagopa.pn.deliverypushworkflow.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.nationalregistries.ApiClient;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.nationalregistries.api.AddressApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.nationalregistries.api.AgenziaEntrateApi;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NationalRegistriesApiReactiveConfigurator extends CommonBaseClient {

    @Bean
    public AgenziaEntrateApi agenziaEntrateApiReactive(PnDeliveryPushWorkflowConfigs cfg){
        return new AgenziaEntrateApi(getNewApiClient(cfg));
    }

    @Bean
    public AddressApi addressApiReactive(PnDeliveryPushWorkflowConfigs cfg){
        return new AddressApi(getNewApiClient(cfg));
    }
    
    @NotNull
    private ApiClient getNewApiClient(PnDeliveryPushWorkflowConfigs cfg) {
        ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()) );
        newApiClient.setBasePath( cfg.getNationalRegistriesBaseUrl() );
        return newApiClient;
    }
}
