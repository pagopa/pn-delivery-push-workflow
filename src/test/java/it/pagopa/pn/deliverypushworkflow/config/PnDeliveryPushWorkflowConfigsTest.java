package it.pagopa.pn.deliverypushworkflow.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

class PnDeliveryPushWorkflowConfigsTest {

    @Test
    void initFailsWhenPlannedAddressTtlIsMissing() {
        PnDeliveryPushWorkflowConfigs configs = configsWith(null);

        Assertions.assertThrows(IllegalStateException.class, configs::init);
    }

    @Test
    void initFailsWhenPlannedAddressTtlIsNotPositive() {
        PnDeliveryPushWorkflowConfigs configs = configsWith(Duration.ZERO);

        Assertions.assertThrows(IllegalStateException.class, configs::init);
    }

    @Test
    void initFailsWhenPlannedAddressTtlDoesNotCoverTheLongestRetryWindow() {
        PnDeliveryPushWorkflowConfigs configs = configsWith(Duration.ofMinutes(75));

        Assertions.assertThrows(IllegalStateException.class, configs::init);
    }

    @Test
    void initSucceedsWhenPlannedAddressTtlExceedsTheLongestRetryWindow() {
        PnDeliveryPushWorkflowConfigs configs = configsWith(Duration.ofMinutes(180));

        Assertions.assertDoesNotThrow(configs::init);
    }

    private PnDeliveryPushWorkflowConfigs configsWith(Duration plannedAddressTtl) {
        PnDeliveryPushWorkflowConfigs.CourtesyRetry.IntervalsMinutes intervalsMinutes = new PnDeliveryPushWorkflowConfigs.CourtesyRetry.IntervalsMinutes();
        intervalsMinutes.setIo(List.of(5, 10, 20, 40));
        intervalsMinutes.setSms(List.of(1, 2, 4, 8));
        intervalsMinutes.setEmail(List.of(1, 2, 4, 8));
        intervalsMinutes.setTpp(List.of(2, 4, 8));

        PnDeliveryPushWorkflowConfigs.CourtesyRetry courtesyRetry = new PnDeliveryPushWorkflowConfigs.CourtesyRetry();
        courtesyRetry.setIntervalsMinutes(intervalsMinutes);
        courtesyRetry.setPlannedAddressTtl(plannedAddressTtl);

        PnDeliveryPushWorkflowConfigs configs = new PnDeliveryPushWorkflowConfigs();
        configs.setCourtesyRetry(courtesyRetry);
        return configs;
    }
}
