package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypushworkflow.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.NotificationFeePolicy;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequest;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.deliverypush.PnDeliveryPushClientReactive;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.externalregistry.PnExternalRegistriesClientReactive;
import it.pagopa.pn.deliverypushworkflow.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypushworkflow.service.mapper.NotificationCostResponseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TOTAL_COST_NOT_PRESENT;

@Service
@Slf4j
public class NotificationProcessCostServiceImpl implements NotificationProcessCostService {
    public static final double MIN_VERSION_PAFEE_VAT_MANDATORY = 2.3;
    private final int sendFee;
    private final PnExternalRegistriesClientReactive pnExternalRegistriesClientReactive;
    private final PnDeliveryPushClientReactive pnDeliveryPushClientReactive;
    
    public NotificationProcessCostServiceImpl(
            PnExternalRegistriesClientReactive pnExternalRegistriesClientReactive,
            PnDeliveryPushWorkflowConfigs cfg,
            PnDeliveryPushClientReactive pnDeliveryPushClientReactive
    ) {
        this.pnDeliveryPushClientReactive = pnDeliveryPushClientReactive;
        this.pnExternalRegistriesClientReactive = pnExternalRegistriesClientReactive;
        this.sendFee = cfg.getPagoPaNotificationBaseCost();
    }

    @Override
    public Mono<Integer> getSendFeeAsync() {
        return Mono.just(sendFee);
    }

    @Override
    public int getSendFee() {
        return sendFee;
    }
    
    public Mono<UpdateNotificationCostResponseInt> setNotificationStepCost(int notificationStepCost,
                                                                         String iun,
                                                                         List<PaymentsInfoForRecipientInt> paymentsInfoForRecipients,
                                                                         Instant eventTimestamp,
                                                                         Instant eventStorageTimestamp,
                                                                         UpdateCostPhaseInt updateCostPhase){
        log.debug("Start service setNotificationStepCost");

        UpdateNotificationCostRequest updateNotificationCostRequest = NotificationCostResponseMapper.internalToExternal(notificationStepCost, iun, paymentsInfoForRecipients, eventTimestamp, eventStorageTimestamp, updateCostPhase);
        return pnExternalRegistriesClientReactive.updateNotificationCost(updateNotificationCostRequest)
                .map(NotificationCostResponseMapper::externalToInternal)
                .doOnSuccess(res -> log.debug("setNotificationStepCost service completed"));
    }
    
    @Override
    public Mono<Integer> notificationProcessCostF24(String iun,
                                                    int recIndex,
                                                    NotificationFeePolicy notificationFeePolicy, 
                                                    Integer paFee,
                                                    Integer vat,
                                                    String version
    ) {
        return pnDeliveryPushClientReactive.getNotificationProcessCost(iun, recIndex, notificationFeePolicy, true, paFee, vat)
                .map(notificationProcessCost -> {
                    log.debug("Get notificationProcessCost response={}", notificationProcessCost);
                    if (notificationProcessCost.getTotalCost() != null){
                        log.info("For F24 can set notification total cost={} - iun={} id={}",notificationProcessCost.getTotalCost(), iun, recIndex );
                        return notificationProcessCost.getTotalCost();
                    } else {
                        log.info("For F24 cannot set notification total cost- iun={} id={}", iun, recIndex);
                        checkIsPossibileCase(iun, recIndex, notificationFeePolicy, version);
                        log.info("For F24 can set notification partial cost={} - iun={} id={}", notificationProcessCost.getPartialCost(), iun, recIndex );
                        return notificationProcessCost.getPartialCost();
                    }
                });
    }

    private static void checkIsPossibileCase(String iun, int recIndex, NotificationFeePolicy notificationFeePolicy, String version) {
        //Dalla versione 2.3 il totalCost deve essere sempre Valorizzato, perchè c'è l'obbligatorietà ed eventualmente default
        Double numberVersion =  version != null ? Double.valueOf(version) : null;
        if(NotificationFeePolicy.DELIVERY_MODE.equals(notificationFeePolicy) && numberVersion != null && numberVersion >= MIN_VERSION_PAFEE_VAT_MANDATORY){
            String msg = String.format("Notification process totalCost is not present and notification version is=%s, can't generate F24 - iun=%s id=%s", version, iun, recIndex);
            log.error(msg);
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_TOTAL_COST_NOT_PRESENT);
        }
    }
}
