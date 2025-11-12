package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.deliverypushworkflow.dto.RaddSearchModeInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.CheckCoverageResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.SearchMode;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.raddalt.RaddAltClient;
import it.pagopa.pn.deliverypushworkflow.service.RaddAltService;
import it.pagopa.pn.deliverypushworkflow.service.mapper.RaddAltMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@AllArgsConstructor
public class RaddAltServiceRestImpl implements RaddAltService {
    private final RaddAltClient raddAltClient;
    private final RaddAltMapper raddAltMapper;

    @Override
    public Boolean checkCoverage(RaddSearchModeInt searchMode, PhysicalAddressInt physicalAddressInt, Instant searchDate) {
        log.info("Starting to check RADD coverage");
        CheckCoverageResponse response = raddAltClient.checkCoverage(SearchMode.fromValue(searchMode.name()),
                raddAltMapper.fromRequestIntToRequestExt(physicalAddressInt), LocalDateTime.ofInstant(searchDate, ZoneOffset.UTC).toLocalDate());
        return response.getHasCoverage();
    }
}
