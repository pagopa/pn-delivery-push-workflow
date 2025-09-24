package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.datavault;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.BaseRecipientDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.datavault_reactive.model.MandateDto;
import reactor.core.publisher.Flux;

import java.util.List;

public interface PnDataVaultClientReactive {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT;
    String GET_RECIPIENT_DENOMINATION = "GET RECIPIENT DENOMINATION";
    String GET_MANDATES_BY_IDS = "GET MANDATES BY IDS";

    Flux<BaseRecipientDto> getRecipientsDenominationByInternalId(List<String> listInternalId);

    Flux<MandateDto> getMandatesByIds(List<String> mandateIds);
}
