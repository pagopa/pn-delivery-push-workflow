package it.pagopa.pn.deliverypushworkflow.exceptions;

import it.pagopa.pn.commons.exceptions.PnInternalException;

import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_FILE_GONE;

public class PnFileGoneException extends PnInternalException {

    public PnFileGoneException(String message, Throwable cause) {
        super(message, ERROR_CODE_DELIVERYPUSH_FILE_GONE, cause);
    }
}
