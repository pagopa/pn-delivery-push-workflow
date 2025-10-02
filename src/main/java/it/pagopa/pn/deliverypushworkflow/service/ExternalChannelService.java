package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.SendInformation;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.DeliveryModeInt;

public interface ExternalChannelService {
    String sendDigitalNotification(NotificationInt notification,
                                   Integer recIndex,
                                   boolean sendAlreadyInProgress,
                                   SendInformation sendInformation);

    void sendCourtesyNotification(NotificationInt notification, CourtesyDigitalAddressInt courtesyAddress, Integer recIndex, String eventId, DeliveryModeInt deliveryMode);

}
