package it.pagopa.pn.deliverypushworkflow.legalfacts;


import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;

public interface AarTemplateChooseStrategy {
    AarTemplateType choose(PhysicalAddressInt address, NotificationInt notificationInt);
}
