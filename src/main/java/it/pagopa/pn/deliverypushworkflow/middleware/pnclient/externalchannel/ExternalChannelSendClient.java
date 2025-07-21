package it.pagopa.pn.deliverypushworkflow.middleware.pnclient.externalchannel;


import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.delivery.notification.NotificationRecipientInt;

import java.util.List;

public interface ExternalChannelSendClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_CHANNELS;
    String LEGAL_NOTIFICATION_REQUEST = "LEGAL NOTIFICATION_REQUEST";
    String COURTESY_NOTIFICATION_REQUEST = "COURTESY NOTIFICATION_REQUEST";


    void sendLegalNotification(NotificationInt notificationInt,
                               NotificationRecipientInt recipientInt,
                               LegalDigitalAddressInt digitalAddress,
                               String timelineEventId,
                               List<String> fileKeys,
                               String quickAccessToken);

    void sendCourtesyNotification(NotificationInt notificationInt,
                                  NotificationRecipientInt recipientInt,
                                  CourtesyDigitalAddressInt digitalAddress,
                                  String timelineEventId,
                                  String aarKey,
                                  String quickAccessToken);

}
