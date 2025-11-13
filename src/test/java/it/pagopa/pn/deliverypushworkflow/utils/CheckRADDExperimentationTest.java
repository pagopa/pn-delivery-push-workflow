package it.pagopa.pn.deliverypushworkflow.utils;

import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.service.CheckCoverageAreaService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

class CheckRADDExperimentationTest {
    private CheckRADDExperimentation checker;
    @Mock
    private CheckCoverageAreaService checkCoverageAreaService;

    @BeforeEach
    void setup() {
        checker = new CheckRADDExperimentation(checkCoverageAreaService);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void checkAddressWithNoState() {
        PhysicalAddressInt addressToCheck = PhysicalAddressInt.builder().foreignState(null).build();
        NotificationInt notificationInt = NotificationInt.builder().build();
        // addressToCheck.set
        boolean isEnabled = checker.checkAddress(addressToCheck,notificationInt);
        Assertions.assertFalse(isEnabled);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void checkAddressWithForeignState() {
        PhysicalAddressInt addressToCheck = PhysicalAddressInt.builder().foreignState("US").build();
        NotificationInt notificationInt = NotificationInt.builder().build();
        boolean isEnabled = checker.checkAddress(addressToCheck,notificationInt);
        Assertions.assertFalse(isEnabled);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void checkAddressWithEmptyZip() {
        PhysicalAddressInt addressToCheck = PhysicalAddressInt.builder().foreignState("iTaLia").build();
        NotificationInt notificationInt = NotificationInt.builder().build();
        boolean isEnabled = checker.checkAddress(addressToCheck,notificationInt);
        Assertions.assertFalse(isEnabled);
    }
    @ExtendWith(MockitoExtension.class)
    @Test
    void notFoundZipInStores() {
        PhysicalAddressInt addressToCheck = PhysicalAddressInt.builder().foreignState("iTaLia").zip("224").build();
        NotificationInt notificationInt = NotificationInt.builder().build();
        boolean isEnabled = checker.checkAddress(addressToCheck,notificationInt);
        Assertions.assertFalse(isEnabled);
    }


}
