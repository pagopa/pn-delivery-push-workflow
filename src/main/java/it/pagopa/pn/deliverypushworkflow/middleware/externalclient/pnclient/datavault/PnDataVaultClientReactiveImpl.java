package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.api.RecipientsApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.BaseRecipientDto;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@CustomLog
public class PnDataVaultClientReactiveImpl extends CommonBaseClient implements PnDataVaultClientReactive {
    private final RecipientsApi recipientsApi;

    @Override
    @Retryable(
            value = {PnInternalException.class},
            maxAttempts = 3,
            backoff = @Backoff(random = true, delay = 500, maxDelay = 1000, multiplier = 2)
    )
    public Flux<BaseRecipientDto> getRecipientsDenominationByInternalId(List<String> listInternalId) {
        log.logInvokingExternalService(CLIENT_NAME, GET_RECIPIENT_DENOMINATION);
        log.debug("Start call getRecipientDenominationByInternalId - listInternalId={}", listInternalId);

        return recipientsApi.getRecipientDenominationByInternalId(listInternalId)
                .onErrorResume( err -> {
                    log.error("Exception invoking getRecipientDenominationByInternalId with internalId list={} err ",listInternalId, err);
                    return Mono.error(new PnInternalException("Exception invoking getRecipientDenominationByInternalId ", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_UPDATEMETAFILEERROR, err));
                });
    }
}
