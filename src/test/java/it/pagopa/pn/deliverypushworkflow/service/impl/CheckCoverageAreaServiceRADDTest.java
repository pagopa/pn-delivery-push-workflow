package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.raddalt.RaddSearchModeInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.service.RaddAltService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class CheckCoverageAreaServiceRADDTest {

    private RaddAltService raddAltService;
    private PnDeliveryPushWorkflowConfigs configs;
    private CheckCoverageAreaServiceRADD service;

    @BeforeEach
    void setUp() {
        raddAltService = mock(RaddAltService.class);
        configs = mock(PnDeliveryPushWorkflowConfigs.class);
        service = new CheckCoverageAreaServiceRADD(raddAltService, configs);
    }

    @Test
    void testIsAreaCovered_returnsTrue() {
        PhysicalAddressInt address = PhysicalAddressInt.builder().zip("1000").build();
        NotificationInt notification = NotificationInt.builder().iun("IUN1").sentAt(Instant.parse("2024-06-01T10:00:00Z")).build();

        when(configs.getRaddSearchMode()).thenReturn(RaddSearchModeInt.LIGHT);
        when(raddAltService.checkCoverage(
                RaddSearchModeInt.LIGHT,
                address,
                Instant.parse("2024-06-01T10:00:00Z")
        )).thenReturn(true);

        boolean result = service.isAreaCovered(address, notification.getSentAt());
        assertTrue(result);
    }

    @Test
    void testIsAreaCovered_returnsFalse() {
        PhysicalAddressInt address = PhysicalAddressInt.builder().zip("2000").build();
        NotificationInt notification = NotificationInt.builder().iun("IUN2").sentAt(Instant.parse("2024-06-02T11:00:00Z")).build();

        when(configs.getRaddSearchMode()).thenReturn(RaddSearchModeInt.COMPLETE);
        when(raddAltService.checkCoverage(
                RaddSearchModeInt.LIGHT,
                address,
                Instant.parse("2024-06-02T11:00:00Z")
        )).thenReturn(false);

        boolean result = service.isAreaCovered(address, notification.getSentAt());
        assertFalse(result);
    }
}

