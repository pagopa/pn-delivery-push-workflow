package it.pagopa.pn.deliverypushworkflow.action.it.mockbean;

import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class PnDeliveryClientReactiveMock implements PnDeliveryClientReactive {

    @Override
    public Mono<Void> removeAllNotificationCostsByIun(String iun) {
        return Mono.empty();
    }
}
