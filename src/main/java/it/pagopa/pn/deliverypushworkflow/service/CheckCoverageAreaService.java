package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;

public interface CheckCoverageAreaService {
    boolean isAreaCovered(PhysicalAddressInt toCheck, NotificationInt notificationInt);
}
