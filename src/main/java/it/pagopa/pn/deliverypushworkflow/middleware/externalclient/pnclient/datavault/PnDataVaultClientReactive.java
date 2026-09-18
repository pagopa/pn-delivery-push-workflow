package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.BaseRecipientDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.MandateDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

public interface PnDataVaultClientReactive {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT;
    String GET_RECIPIENT_DENOMINATION = "GET RECIPIENT DENOMINATION";
    String GET_MANDATES_BY_IDS = "GET MANDATES BY IDS";
    String UPDATE_RECIPIENT_ADDRESS = "UPDATE RECIPIENT ADDRESS";
    String GET_RECIPIENT_ADDRESS = "GET RECIPIENT ADDRESS";

    Flux<BaseRecipientDto> getRecipientsDenominationByInternalId(List<String> listInternalId);

    Flux<MandateDto> getMandatesByIds(List<String> mandateIds);

    Mono<Void> updateRecipientAddress(String internalId, String addressId, BigDecimal ttlSeconds, String address);

    Mono<String> getRecipientAddress(String internalId, String addressId);
}
