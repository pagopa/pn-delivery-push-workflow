package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.notificationcostservice;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.notificationcostservice_reactive.model.PaperCostToInvalidate;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface NotificationCostServiceClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_NOTIFICATION_COST_SERVICE;
    String INVALIDATE_NOTIFICATION_COST = "INVALIDATE NOTIFICATION COST";

    Mono<ResponseEntity<Void>> invalidatePaperCostWithHttpInfo(String iun, PaperCostToInvalidate paperCostToInvalidate);
}
 