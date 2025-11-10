package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.dto.RaddSearchModeInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;

import java.time.Instant;

public interface RaddAltService {
    Boolean checkCoverage(RaddSearchModeInt searchMode, PhysicalAddressInt physicalAddressInt, Instant searchDate);
}
