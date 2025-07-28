package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypushworkflow.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.NotificationFeePolicy;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public interface NotificationProcessCostService {
    Mono<UpdateNotificationCostResponseInt> setNotificationStepCost(int notificationStepCost,
                                                                           String iun,
                                                                           List<PaymentsInfoForRecipientInt> paymentsInfoForRecipients,
                                                                           Instant eventTimestamp,
                                                                           Instant eventStorageTimestamp,
                                                                           UpdateCostPhaseInt updateCostPhase);
    
    Mono<Integer> getSendFeeAsync();
    int getSendFee();
    Mono<Integer> notificationProcessCostF24(String iun, int recIndex, NotificationFeePolicy notificationFeePolicy, Integer paFee, Integer vat, String version);
}
