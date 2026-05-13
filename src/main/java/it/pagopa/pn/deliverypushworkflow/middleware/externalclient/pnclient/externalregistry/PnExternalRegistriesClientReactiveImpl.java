package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.externalregistry;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.PagoPaIntMode;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.api.PaperCostApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.api.UpdateNotificationCostApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.PaperCostToInvalidate;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@CustomLog
public class PnExternalRegistriesClientReactiveImpl extends CommonBaseClient implements PnExternalRegistriesClientReactive {
    private final UpdateNotificationCostApi updateNotificationCostApi;
    private final PaperCostApi paperCostApi;

    public Mono<UpdateNotificationCostResponse> updateNotificationCost(UpdateNotificationCostRequest updateNotificationCostRequest) {
        return updateNotificationCostApi.updateNotificationCost(updateNotificationCostRequest);
    }

    public Mono<ResponseEntity<Void>> invalidatePaperCostWithHttpInfo(String iun, PaperCostToInvalidate paperCostToInvalidate, PagoPaIntMode mode) {
        if (!PagoPaIntMode.ASYNC.equals(mode)) {
            log.debug("Invalidating not possible for mode: {}", mode);
            return Mono.empty();
        }
        return paperCostApi.invalidatePaperCostWithHttpInfo(iun, paperCostToInvalidate);
    }

}
