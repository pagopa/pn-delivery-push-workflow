package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.deliverypush;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.NotificationFeePolicy;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.deliverypush.model.NotificationProcessCostResponse;
import reactor.core.publisher.Mono;

public interface PnDeliveryPushClientReactive {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_DELIVERY_PUSH;
    String GET_NOTIFICATION_PROCESS_COST = "GET NOTIFICATION PROCESS COST";

    Mono<NotificationProcessCostResponse> getNotificationProcessCost(
            String iun,
            Integer recIndex,
            NotificationFeePolicy notificationFeePolicy,
            Boolean applyCost,
            Integer paFee,
            Integer vat
    );
}
