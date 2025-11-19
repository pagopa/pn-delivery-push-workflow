package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.service.CheckCoverageAreaService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "pn.delivery-push-workflow.radd-search-mode", havingValue = "OLD")
public class CheckCoverageAreaServiceParameterStore implements CheckCoverageAreaService {

    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;
    private final ParameterConsumer parameterConsumer;

    @Override
    public boolean isAreaCovered(PhysicalAddressInt toCheck, Instant dateToCheck) {
        log.info("CheckAreaService initialized with searchMode={}, dateToCheck={}", pnDeliveryPushWorkflowConfigs.getRaddSearchMode()
                ,dateToCheck);
        // country in admitted countries
        List<String> storeNames = pnDeliveryPushWorkflowConfigs.getRaddExperimentationStoresName();
        if (storeNames == null) return false;
        for (String currentStore : storeNames) {
            log.info("Current Store {}", currentStore);
            if (isInStore(toCheck.getZip(), currentStore)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInStore(String zipCode, String storeName) {
        log.trace("Looking for zip code={} in store {}", zipCode, storeName);
        @SuppressWarnings("unchecked") Optional<Set<String>> zipLists = parameterConsumer.getParameterValue(storeName, (Class<Set<String>>) (Object) Set.class);
        if (zipLists.isPresent()) {
            Set<String> experimentalZipList = zipLists.get();
            log.trace("ZipCode ({}) in experimental list? {}", zipCode, experimentalZipList.contains(zipCode));
            return experimentalZipList.contains(zipCode);
        }
        log.trace("ZipCode ({}) not found in experimental list", zipCode);
        return false;
    }
}
