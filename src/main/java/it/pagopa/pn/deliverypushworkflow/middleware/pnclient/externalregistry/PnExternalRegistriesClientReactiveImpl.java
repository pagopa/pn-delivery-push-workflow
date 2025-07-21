package it.pagopa.pn.deliverypushworkflow.middleware.pnclient.externalregistry;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.api.UpdateNotificationCostApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequestDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResponseDto;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@CustomLog
public class PnExternalRegistriesClientReactiveImpl extends CommonBaseClient implements PnExternalRegistriesClientReactive {
    private final UpdateNotificationCostApi updateNotificationCostApi;

    public Mono<UpdateNotificationCostResponseDto> updateNotificationCost(UpdateNotificationCostRequestDto updateNotificationCostRequest) {
        return updateNotificationCostApi.updateNotificationCost(updateNotificationCostRequest);
    }

}
