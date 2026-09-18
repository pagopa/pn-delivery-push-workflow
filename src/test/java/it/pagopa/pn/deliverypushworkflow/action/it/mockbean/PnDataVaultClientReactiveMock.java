package it.pagopa.pn.deliverypushworkflow.action.it.mockbean;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.BaseRecipientDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.MandateDto;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.datavault.PnDataVaultClientReactive;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class PnDataVaultClientReactiveMock implements PnDataVaultClientReactive {
    private ConcurrentMap<String, BaseRecipientDto> confidentialMap;
    private ConcurrentMap<String, String> addressMap;

    public void clear() {
        this.confidentialMap = new ConcurrentHashMap<>();
        this.addressMap = new ConcurrentHashMap<>();
    }
    
    public void insertBaseRecipientDto(BaseRecipientDto dto){
        confidentialMap.put(dto.getInternalId(), dto);
    }

    @Override
    public Flux<BaseRecipientDto> getRecipientsDenominationByInternalId(List<String> listInternalId) {
        return Flux.fromStream(listInternalId.stream()
                .filter( internalId -> confidentialMap.get(internalId) != null)
                .map(internalId -> confidentialMap.get(internalId)));
    }

    @Override
    public Flux<MandateDto> getMandatesByIds(List<String> mandateIds) {
        return null;
    }

    @Override
    public Mono<Void> updateRecipientAddress(String internalId, String addressId, BigDecimal ttlSeconds, String address) {
        addressMap.put(buildKey(internalId, addressId), address);
        return Mono.empty();
    }

    @Override
    public Mono<String> getRecipientAddress(String internalId, String addressId) {
        return Mono.justOrEmpty(addressMap.get(buildKey(internalId, addressId)));
    }

    public void removeRecipientAddress(String internalId, String addressId) {
        addressMap.remove(buildKey(internalId, addressId));
    }

    private String buildKey(String internalId, String addressId) {
        return internalId + "##" + addressId;
    }

}