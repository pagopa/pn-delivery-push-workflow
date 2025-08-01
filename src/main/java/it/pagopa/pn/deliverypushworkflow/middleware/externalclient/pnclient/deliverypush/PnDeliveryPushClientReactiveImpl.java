package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.deliverypush;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.NotificationFeePolicy;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.deliverypush.api.NotificationProcessCostApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.deliverypush.model.NotificationProcessCostResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@CustomLog
@RequiredArgsConstructor
public class PnDeliveryPushClientReactiveImpl implements PnDeliveryPushClientReactive {
    private final NotificationProcessCostApi notificationProcessCostApi;

    public Mono<NotificationProcessCostResponse> getNotificationProcessCost(
            String iun,
            Integer recIndex,
            NotificationFeePolicy notificationFeePolicy,
            Boolean applyCost,
            Integer paFee,
            Integer vat
    ) {

        log.logInvokingExternalService(CLIENT_NAME, GET_NOTIFICATION_PROCESS_COST);
        return notificationProcessCostApi.notificationProcessCost(
                iun,
                recIndex,
                it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.deliverypush.model.NotificationFeePolicy.fromValue(notificationFeePolicy.getValue()),
                applyCost,
                paFee,
                vat
        );
    }

}
