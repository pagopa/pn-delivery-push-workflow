package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.api.AddressBookApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.api.MandatesApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.api.RecipientsApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.AddressDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.BaseRecipientDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.MandateDto;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@CustomLog
public class PnDataVaultClientReactiveImpl extends CommonBaseClient implements PnDataVaultClientReactive {
    private final RecipientsApi recipientsApi;
    private final MandatesApi mandatesApi;
    private final AddressBookApi addressBookApi;

    @Override
    @Retryable(
            retryFor = {PnInternalException.class},
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

    @Override
    @Retryable(
            retryFor = {PnInternalException.class},
            backoff = @Backoff(random = true, delay = 500, maxDelay = 1000, multiplier = 2)
    )
    public Flux<MandateDto> getMandatesByIds(List<String> mandateIds) {
        log.logInvokingExternalService(CLIENT_NAME, GET_MANDATES_BY_IDS);
        return mandatesApi.getMandatesByIds(mandateIds)
                .onErrorResume( err -> {
                    log.error("Exception invoking getMandatesByIds with mandateIds list={} err ", mandateIds, err);
                    return Mono.error(new PnInternalException("Exception invoking getMandatesByIds ", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_DATAVAULTMANDATESERROR, err));
                });
    }

    @Override
    @Retryable(
            retryFor = {PnInternalException.class},
            backoff = @Backoff(random = true, delay = 500, maxDelay = 1000, multiplier = 2)
    )
    public Mono<Void> updateRecipientAddress(String internalId, String addressId, BigDecimal ttlSeconds, String address) {
        log.logInvokingExternalService(CLIENT_NAME, UPDATE_RECIPIENT_ADDRESS);
        log.debug("Start call updateRecipientAddress - addressId={}", addressId);

        return addressBookApi.updateRecipientAddressByInternalId(internalId, addressId, ttlSeconds, new AddressDto().value(address))
                .onErrorResume( err -> {
                    log.error("Exception invoking updateRecipientAddress with addressId={} err ", addressId, err);
                    return Mono.error(new PnInternalException("Exception invoking updateRecipientAddress ", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_DATAVAULTADDRESSERROR, err));
                });
    }

    @Override
    @Retryable(
            retryFor = {PnInternalException.class},
            backoff = @Backoff(random = true, delay = 500, maxDelay = 1000, multiplier = 2)
    )
    public Mono<String> getRecipientAddress(String internalId, String addressId) {
        log.logInvokingExternalService(CLIENT_NAME, GET_RECIPIENT_ADDRESS);
        log.debug("Start call getRecipientAddress - addressId={}", addressId);

        return addressBookApi.getRecipientAddressesByInternalId(internalId)
                .onErrorResume( err -> {
                    log.error("Exception invoking getRecipientAddress with addressId={} err ", addressId, err);
                    return Mono.error(new PnInternalException("Exception invoking getRecipientAddress ", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_DATAVAULTADDRESSERROR, err));
                })
                .mapNotNull(recipientAddresses -> {
                    Map<String, AddressDto> addresses = recipientAddresses.getAddresses();
                    if (addresses == null) {
                        return null;
                    }
                    AddressDto addressDto = addresses.get(addressId);
                    return addressDto != null ? addressDto.getValue() : null;
                });
    }
}
