package it.pagopa.pn.deliverypushworkflow.exception;

import it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class PnDeliveryPushExceptionCodesTest {

    @Test
    void checkAll() {
        Assertions.assertAll(
                () -> Assertions.assertEquals("PN_DELIVERYPUSH_NOTFOUND", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTFOUND),
                () -> Assertions.assertEquals("PN_DELIVERYPUSH_UPLOADFILEERROR", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_UPLOADFILEERROR)
        );
    }

}