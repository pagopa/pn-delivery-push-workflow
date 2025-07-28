package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.ContactPhaseInt;

public interface NationalRegistriesService {
    void sendRequestForGetDigitalGeneralAddress(NotificationInt notification,
                                                Integer recIndex, 
                                                ContactPhaseInt contactPhase,
                                                int sentAttemptMade,
                                                String relatedFeedbackTimelineId);
}
