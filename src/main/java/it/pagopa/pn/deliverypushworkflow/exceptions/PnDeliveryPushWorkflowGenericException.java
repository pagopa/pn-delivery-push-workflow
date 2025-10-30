package it.pagopa.pn.deliverypushworkflow.exceptions;

import it.pagopa.pn.commons.exceptions.PnInternalException;

public class PnDeliveryPushWorkflowGenericException extends PnInternalException {
    public PnDeliveryPushWorkflowGenericException(String message, String errorCode) {
        super(message, errorCode);
    }

    public PnDeliveryPushWorkflowGenericException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
