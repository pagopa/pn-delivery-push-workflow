package it.pagopa.pn.deliverypushworkflow.dto.raddalt;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.SearchMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RaddSearchModeTest {
    @Test
    void toClientSearchModeReturnsLightForLightEnum() {
        Assertions.assertEquals(SearchMode.LIGHT, RaddSearchModeInt.LIGHT.toClientSearchMode());
    }

    @Test
    void toClientSearchModeReturnsCompleteForCompleteEnum() {
        Assertions.assertEquals(SearchMode.COMPLETE, RaddSearchModeInt.COMPLETE.toClientSearchMode());
    }

    @Test
    void toClientSearchModeThrowsExceptionForOldEnum() {
        assertThrows(
                IllegalStateException.class,
                RaddSearchModeInt.OLD::toClientSearchMode
        );
    }
}
