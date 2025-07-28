package it.pagopa.pn.deliverypushworkflow.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERY_PUSH_NOTIFICATION_NOT_ACCEPTED;

public class PnNotificationNotAcceptedException extends PnRuntimeException {

    public PnNotificationNotAcceptedException() {
        this(null);
    }

    public PnNotificationNotAcceptedException(Throwable ex) {
        super("Notification not ACCEPTED", "Notification has not been ACCEPTED", HttpStatus.NOT_FOUND.value(), ERROR_CODE_DELIVERY_PUSH_NOTIFICATION_NOT_ACCEPTED, null, null, ex);
    }

}
