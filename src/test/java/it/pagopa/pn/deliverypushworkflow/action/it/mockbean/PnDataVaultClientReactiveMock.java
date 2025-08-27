package it.pagopa.pn.deliverypushworkflow.action.it.mockbean;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.BaseRecipientDto;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.datavault.PnDataVaultClientReactive;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class PnDataVaultClientReactiveMock implements PnDataVaultClientReactive {
    private ConcurrentMap<String, BaseRecipientDto> confidentialMap;

    public void clear() {
        this.confidentialMap = new ConcurrentHashMap<>();
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

}