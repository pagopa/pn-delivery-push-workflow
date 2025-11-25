package it.pagopa.pn.deliverypushworkflow.action.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReworkIdBuilderTest {

    @Test
    @DisplayName("extractTryIdx returns correct value when TRY pattern is present")
    void extractTryIdxWithValidPattern() {
        String reworkId = "REWORK_2.TRY_5";
        Integer result = ReworkIdBuilder.extractTryIdx(reworkId);
        Assertions.assertEquals(5, result);
    }

    @Test
    @DisplayName("extractTryIdx returns 0 when TRY pattern is missing")
    void extractTryIdxWithMissingPattern() {
        String reworkId = "REWORK_2";
        Integer result = ReworkIdBuilder.extractTryIdx(reworkId);
        Assertions.assertEquals(0, result);
    }

    @Test
    @DisplayName("extractTryIdx returns 0 when TRY pattern is malformed")
    void extractTryIdxWithMalformedPattern() {
        String reworkId = "REWORK_2.TRY_";
        Integer result = ReworkIdBuilder.extractTryIdx(reworkId);
        Assertions.assertEquals(0, result);
    }

    @Test
    @DisplayName("extractReworkIdx returns correct value when REWORK pattern is present")
    void extractReworkIdxWithValidPattern() {
        String reworkId = "REWORK_3.TRY_7";
        Integer result = ReworkIdBuilder.extractReworkIdx(reworkId);
        Assertions.assertEquals(3, result);
    }

    @Test
    @DisplayName("extractReworkIdx returns 0 when REWORK pattern is missing")
    void extractReworkIdxWithMissingPattern() {
        String reworkId = "TRY_7";
        Integer result = ReworkIdBuilder.extractReworkIdx(reworkId);
        Assertions.assertEquals(0, result);
    }

    @Test
    @DisplayName("extractReworkIdx returns 0 when REWORK pattern is malformed")
    void extractReworkIdxWithMalformedPattern() {
        String reworkId = "REWORK_.TRY_7";
        Integer result = ReworkIdBuilder.extractReworkIdx(reworkId);
        Assertions.assertEquals(0, result);
    }

    @Test
    @DisplayName("build returns correct formatted string")
    void buildReturnsCorrectString() {
        String result = ReworkIdBuilder.build(4, 9);
        Assertions.assertEquals("REWORK_4.TRY_9", result);
    }

    @Test
    @DisplayName("build works with zero values")
    void buildWithZeroValues() {
        String result = ReworkIdBuilder.build(0, 0);
        Assertions.assertEquals("REWORK_0.TRY_0", result);
    }
}