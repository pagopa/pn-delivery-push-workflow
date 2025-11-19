package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.deliverypushworkflow.dto.raddalt.RaddSearchModeInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.CheckCoverageRequest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.CheckCoverageResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.SearchMode;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.raddalt.RaddAltClient;
import it.pagopa.pn.deliverypushworkflow.service.mapper.RaddAltMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class RaddAltServiceRestImplTest {

    private RaddAltClient raddAltClient;
    private RaddAltMapper raddAltMapper;
    private RaddAltServiceRestImpl service;

    @BeforeEach
    void setUp() {
        raddAltClient = mock(RaddAltClient.class);
        raddAltMapper = mock(RaddAltMapper.class);
        service = new RaddAltServiceRestImpl(raddAltClient, raddAltMapper);
    }

    @Test
    void testCheckCoverage_ReturnsTrue() {
        RaddSearchModeInt searchMode = RaddSearchModeInt.COMPLETE;
        PhysicalAddressInt addressInt = mock(PhysicalAddressInt.class);
        CheckCoverageRequest mappedAddress = new CheckCoverageRequest();
        Instant searchDate = Instant.now();

        when(raddAltMapper.fromRequestIntToRequestExt(addressInt)).thenReturn(mappedAddress);

        CheckCoverageResponse response = new CheckCoverageResponse();
        response.setHasCoverage(true);

        when(raddAltClient.checkCoverage(
                eq(SearchMode.fromValue(searchMode.name())),
                eq(mappedAddress),
                eq(LocalDateTime.ofInstant(searchDate, ZoneOffset.UTC).toLocalDate())
        )).thenReturn(response);

        Boolean result = service.checkCoverage(searchMode, addressInt, searchDate);

        assertTrue(result);
        verify(raddAltClient).checkCoverage(
                SearchMode.fromValue(searchMode.name()),
                mappedAddress,
                LocalDateTime.ofInstant(searchDate, ZoneOffset.UTC).toLocalDate()
        );
    }

}
