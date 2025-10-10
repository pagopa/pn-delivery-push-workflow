package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry.model.SendMessageResponse;

import java.time.Instant;

public interface IoService {
    SendMessageResponse.ResultEnum sendIOMessage(NotificationInt notification, int recIndex, Instant schedulingAnalogDate, DeliveryModeInt deliveryMode);
}
