package it.pagopa.pn.deliverypushworkflow.utils;

import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FeatureEnabledUtilsTest {

    private PnDeliveryPushWorkflowConfigs configs;
    private FeatureEnabledUtils utils;

    @BeforeEach
    void setUp() {
        configs = Mockito.mock(PnDeliveryPushWorkflowConfigs.class);
        utils = new FeatureEnabledUtils(configs);
    }

    @Test
    void testIsPfNewWorkflowEnabled_True() {
        Instant start = Instant.parse("2024-06-01T00:00:00Z");
        Instant stop = Instant.parse("2024-06-30T00:00:00Z");
        Instant sentAt = Instant.parse("2024-06-15T00:00:00Z");
        Mockito.when(configs.getPfNewWorkflowStart()).thenReturn(start.toString());
        Mockito.when(configs.getPfNewWorkflowStop()).thenReturn(stop.toString());

        assertTrue(utils.isPfNewWorkflowEnabled(sentAt));
    }

    @Test
    void testIsPfNewWorkflowEnabled_False() {
        Instant start = Instant.parse("2024-06-01T00:00:00Z");
        Instant stop = Instant.parse("2024-06-30T00:00:00Z");
        Instant sentAt = Instant.parse("2024-07-01T00:00:00Z");
        Mockito.when(configs.getPfNewWorkflowStart()).thenReturn(start.toString());
        Mockito.when(configs.getPfNewWorkflowStop()).thenReturn(stop.toString());

        assertFalse(utils.isPfNewWorkflowEnabled(sentAt));
    }

    @Test
    void testIsFeatureAAROnlyPECForRADDAndPFEnabled_True() {
        Mockito.when(configs.getAarOnlyPecForRaddAndPf()).thenReturn("true");
        assertTrue(utils.isFeatureAAROnlyPECForRADDAndPFEnabled());
    }

    @Test
    void testIsFeatureAAROnlyPECForRADDAndPFEnabled_False() {
        Mockito.when(configs.getAarOnlyPecForRaddAndPf()).thenReturn("false");
        assertFalse(utils.isFeatureAAROnlyPECForRADDAndPFEnabled());
    }

    @Test
    void testIsFeatureAAROnlyPECForRADDAndPFEnabled_Null() {
        Mockito.when(configs.getAarOnlyPecForRaddAndPf()).thenReturn(null);
        assertFalse(utils.isFeatureAAROnlyPECForRADDAndPFEnabled());
    }

    @Test
    void testIsAnalogWorkflowTimeoutFeatureEnabled_True() {
        Instant start = Instant.parse("2024-06-01T00:00:00Z");
        Instant sentAt = Instant.parse("2024-06-15T00:00:00Z");
        Mockito.when(configs.getStartAnalogWorkflowTimeoutFeatureDate()).thenReturn(start);

        assertTrue(utils.isAnalogWorkflowTimeoutFeatureEnabled(sentAt));
    }

    @Test
    void testIsAnalogWorkflowTimeoutFeatureEnabled_False() {
        Instant start = Instant.parse("2024-06-15T00:00:00Z");
        Instant sentAt = Instant.parse("2024-06-01T00:00:00Z");
        Mockito.when(configs.getStartAnalogWorkflowTimeoutFeatureDate()).thenReturn(start);

        assertFalse(utils.isAnalogWorkflowTimeoutFeatureEnabled(sentAt));
    }

    @Test
    void testIsAnalogWorkflowTimeoutFeatureEnabled_NullStartDate() {
        Instant sentAt = Instant.now();
        Mockito.when(configs.getStartAnalogWorkflowTimeoutFeatureDate()).thenReturn(null);

        assertFalse(utils.isAnalogWorkflowTimeoutFeatureEnabled(sentAt));
    }
}
