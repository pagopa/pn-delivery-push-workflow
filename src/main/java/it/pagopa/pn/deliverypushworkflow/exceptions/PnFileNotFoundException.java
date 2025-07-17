package it.pagopa.pn.deliverypushworkflow.exceptions;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.Getter;

import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_FILE_NOT_FOUND;

@Getter
public class PnFileNotFoundException extends PnInternalException {

    public PnFileNotFoundException(String message, Throwable cause) {
        super(message, ERROR_CODE_DELIVERYPUSH_FILE_NOT_FOUND, cause);
    }

}
