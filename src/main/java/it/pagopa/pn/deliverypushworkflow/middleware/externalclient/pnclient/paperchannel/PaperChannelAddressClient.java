package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.paperchannel;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.model.CheckAddressResponse;
import reactor.core.publisher.Mono;

public interface PaperChannelAddressClient {

    Mono<CheckAddressResponse> checkAddress(String requestId);
}
