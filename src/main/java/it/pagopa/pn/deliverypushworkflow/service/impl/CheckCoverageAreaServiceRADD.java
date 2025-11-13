package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.service.CheckCoverageAreaService;
import it.pagopa.pn.deliverypushworkflow.service.RaddAltService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
@ConditionalOnExpression(
        "'${pn.delivery-push-workflow.radd-search-mode}'.equals('LIGHT') or '${pn.delivery-push-workflow.radd-search-mode}'.equals('COMPLETE')"
)
public class CheckCoverageAreaServiceRADD implements CheckCoverageAreaService {

    private final RaddAltService raddAltClient;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;


    @Override
    public boolean isAreaCovered(PhysicalAddressInt toCheck, NotificationInt notificationInt) {
        log.info("CheckAreaServiceWithRadd initialized with searchMode={}, iun={}, notificationSentAt={}", pnDeliveryPushWorkflowConfigs.getRaddSearchMode()
                ,notificationInt.getIun(),notificationInt.getSentAt());
        return raddAltClient.checkCoverage(pnDeliveryPushWorkflowConfigs.getRaddSearchMode()
                ,toCheck,notificationInt.getSentAt());
    }
}
