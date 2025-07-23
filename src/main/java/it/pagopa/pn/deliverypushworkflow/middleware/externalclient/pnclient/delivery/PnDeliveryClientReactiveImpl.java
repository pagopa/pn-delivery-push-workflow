package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery_reactive.api.InternalOnlyApi;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@CustomLog
public class PnDeliveryClientReactiveImpl extends CommonBaseClient implements PnDeliveryClientReactive{
    private final InternalOnlyApi pnDeliveryApi;

    @Override
    public Mono<Void> removeAllNotificationCostsByIun(String iun) {
        log.logInvokingExternalService(CLIENT_NAME, REMOVE_IUV);

        return pnDeliveryApi.removeAllNotificationCostsByIun(iun)
                .doOnSuccess(res -> log.debug("Received sync response from {} for {} ", CLIENT_NAME, REMOVE_IUV));
    }

}
