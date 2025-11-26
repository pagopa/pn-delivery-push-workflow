package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.paperchannel;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.api.CheckAddressApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.model.CheckAddressResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@CustomLog
public class PaperChannelAddressClientImpl implements  PaperChannelAddressClient{

    private final CheckAddressApi checkAddressApi;

    @Override
    public Mono<CheckAddressResponse> checkAddress(String requestId) {
        return Mono.fromRunnable(() -> checkAddressApi.checkAddress(requestId));
    }
}
