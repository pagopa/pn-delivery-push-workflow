package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;

import java.time.Instant;

public interface CheckCoverageAreaService {
    boolean isAreaCovered(PhysicalAddressInt toCheck, Instant dateToCheck);
}
