package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.externalregistry;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.PagoPaIntMode;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.PaperCostToInvalidate;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResponse;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface PnExternalRegistriesClientReactive {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_REGISTRIES;

    Mono<UpdateNotificationCostResponse> updateNotificationCost(UpdateNotificationCostRequest updateNotificationCostRequest);
    Mono<ResponseEntity<Void>> invalidatePaperCostWithHttpInfo(String iun, PaperCostToInvalidate paperCostToInvalidate, PagoPaIntMode mode);
}
