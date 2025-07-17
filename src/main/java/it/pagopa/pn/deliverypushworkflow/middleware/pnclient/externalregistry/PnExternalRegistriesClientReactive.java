package it.pagopa.pn.deliverypushworkflow.middleware.pnclient.externalregistry;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequestDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResponseDto;
import reactor.core.publisher.Mono;

public interface PnExternalRegistriesClientReactive {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_REGISTRIES;

    Mono<UpdateNotificationCostResponseDto> updateNotificationCost(UpdateNotificationCostRequestDto updateNotificationCostRequest);
}
