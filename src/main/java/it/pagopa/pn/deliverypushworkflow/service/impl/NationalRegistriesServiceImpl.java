package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.deliverypushworkflow.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.nationalregistries.NationalRegistriesClient;
import it.pagopa.pn.deliverypushworkflow.service.NationalRegistriesService;
import it.pagopa.pn.deliverypushworkflow.service.utils.PublicRegistryUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NationalRegistriesServiceImpl implements NationalRegistriesService {
    private final PublicRegistryUtils publicRegistryUtils;
    private final NationalRegistriesClient nationalRegistriesClient;
    private final NotificationUtils notificationUtils;

    public NationalRegistriesServiceImpl(PublicRegistryUtils publicRegistryUtils,
                                         NationalRegistriesClient nationalRegistriesClient,
                                         NotificationUtils notificationUtils) {
        this.publicRegistryUtils = publicRegistryUtils;
        this.nationalRegistriesClient = nationalRegistriesClient;
        this.notificationUtils = notificationUtils;
    }

    /**
     * Send get request to public registry for get digital address
     **/
    @Override
    public void sendRequestForGetDigitalGeneralAddress(NotificationInt notification, 
                                                       Integer recIndex,
                                                       ContactPhaseInt contactPhase, 
                                                       int sentAttemptMade,
                                                       String relatedFeedbackTimelineId) {

        String correlationId = publicRegistryUtils.generateCorrelationId(notification.getIun(), recIndex, contactPhase, sentAttemptMade, DeliveryModeInt.DIGITAL);
        log.debug("Start Async Request for get general address, correlationId={} - iun={} id={}", correlationId, notification.getIun(), recIndex);

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);

        nationalRegistriesClient.sendRequestForGetDigitalAddress(recipient.getTaxId(), recipient.getRecipientType().getValue(), correlationId, notification.getSentAt());
        publicRegistryUtils.addPublicRegistryCallToTimeline(
                notification,
                recIndex,
                contactPhase,
                sentAttemptMade,
                correlationId, 
                DeliveryModeInt.DIGITAL, 
                relatedFeedbackTimelineId);

        log.debug("End sendRequestForGetAddress correlationId={} - iun={} id={}", correlationId, notification.getIun(), recIndex);
    }
}
