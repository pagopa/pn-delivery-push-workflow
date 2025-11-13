package it.pagopa.pn.deliverypushworkflow.utils;

import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.service.CheckCoverageAreaService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CheckRADDExperimentationTest {
    private CheckRADDExperimentation checker;
    @Mock
    private CheckCoverageAreaService checkCoverageAreaService;

    @BeforeEach
    void setup() {
        checker = new CheckRADDExperimentation(checkCoverageAreaService);
    }

    @ParameterizedTest
    @MethodSource("addressProvider")
    void checkAddressWithVariousStates(String foreignState, String zip) {
        PhysicalAddressInt.PhysicalAddressIntBuilder builder = PhysicalAddressInt.builder().foreignState(foreignState);
        if (zip != null) {
            builder.zip(zip);
        }
        PhysicalAddressInt addressToCheck = builder.build();
        NotificationInt notificationInt = NotificationInt.builder().build();
        boolean isEnabled = checker.checkAddress(addressToCheck, notificationInt);
        Assertions.assertFalse(isEnabled);
    }

    static Stream<Arguments> addressProvider() {
        return Stream.of(Arguments.of(null, null),      // No state
                Arguments.of("US", null),      // Foreign state
                Arguments.of("iTaLia", null)   // Empty zip
        );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void notFoundZipInStores() {
        PhysicalAddressInt addressToCheck = PhysicalAddressInt.builder().foreignState("iTaLia").zip("224").build();
        NotificationInt notificationInt = NotificationInt.builder().build();
        boolean isEnabled = checker.checkAddress(addressToCheck, notificationInt);
        Assertions.assertFalse(isEnabled);
    }
}
