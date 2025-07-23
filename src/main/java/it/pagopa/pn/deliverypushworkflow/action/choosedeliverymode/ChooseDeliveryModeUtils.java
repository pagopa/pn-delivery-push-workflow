package it.pagopa.pn.deliverypushworkflow.action.choosedeliverymode;

import it.pagopa.pn.deliverypushworkflow.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendCourtesyMessageDetailsInt;

import java.time.Instant;
import java.util.Optional;

public interface ChooseDeliveryModeUtils {

    void addAvailabilitySourceToTimeline(Integer recIndex, NotificationInt notification, DigitalAddressSourceInt addressSource, boolean isAvailable);

    void addScheduleAnalogWorkflowToTimeline(Integer recIndex, NotificationInt notification, Instant schedulingDate);

    Optional<LegalDigitalAddressInt> getPlatformAddress(NotificationInt notification, Integer recIndex);

    LegalDigitalAddressInt getDigitalDomicile(NotificationInt notification, Integer recIndex);

    Optional<LegalDigitalAddressInt> retrievePlatformAddress(NotificationInt notification, Integer recIndex);

    LegalDigitalAddressInt retrieveSpecialAddress(NotificationInt notification, Integer recIndex);

}