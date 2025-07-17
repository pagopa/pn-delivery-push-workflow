package it.pagopa.pn.deliverypushworkflow.middleware.pnclient.datavault;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.BaseRecipientDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.ConfidentialTimelineElementDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.ConfidentialTimelineElementId;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.NotificationRecipientAddressesDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PnDataVaultClientReactive {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT;
    String GET_RECIPIENT_DENOMINATION = "GET RECIPIENT DENOMINATION";
    String UPDATE_NOTIFICATION_ADDRESS = "UPDATE CONFIDENTIAL INFO, NOTIFICATION ADDRESS";
    String NOTIFICATION_TIMELINES_ADDRESS = "RETRIEVE CONFIDENTIAL INFO, NOTIFICATION TIMELINES";

    Flux<BaseRecipientDto> getRecipientsDenominationByInternalId(List<String> listInternalId);

    Flux<ConfidentialTimelineElementDto> getNotificationTimelines(List<ConfidentialTimelineElementId> confidentialTimelineElementId);
    Mono<Void> updateNotificationAddressesByIun(String iun, Boolean normalized, List<NotificationRecipientAddressesDto> list);
}
