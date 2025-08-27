package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.commons.log.PnLogger;
import reactor.core.publisher.Mono;

public interface PnDeliveryClientReactive {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_DELIVERY;
    String REMOVE_IUV = "REMOVE IUV";

    Mono<Void> removeAllNotificationCostsByIun(String iun);
}
