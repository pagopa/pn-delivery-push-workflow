package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.notificationcostservice;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.notificationcostservice_reactive.api.PaperCostApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.notificationcostservice_reactive.model.PaperCostToInvalidate;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@CustomLog
public class NotificationCostServiceClientImpl extends CommonBaseClient implements NotificationCostServiceClient {

    private final PaperCostApi paperCostApi;

    @Override
    public Mono<ResponseEntity<Void>> invalidatePaperCostWithHttpInfo(String iun, PaperCostToInvalidate paperCostToInvalidate) {
        log.logInvokingAsyncExternalService(CLIENT_NAME, INVALIDATE_NOTIFICATION_COST, iun);

        return paperCostApi.invalidatePaperCostWithHttpInfo(iun, paperCostToInvalidate)
                .doOnError(throwable -> log.error("Error calling invalidatePaperCost with iun: {}", iun, throwable));
    }

}