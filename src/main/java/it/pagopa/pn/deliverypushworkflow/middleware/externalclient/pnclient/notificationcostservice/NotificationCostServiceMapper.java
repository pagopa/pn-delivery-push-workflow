package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.notificationcostservice;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.notificationcostservice_reactive.model.AnalogUpdateCostPhase;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.notificationcostservice_reactive.model.PaperCostToInvalidate;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkConstant.ATTEMPT_0;
import static it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkConstant.ATTEMPT_1;

public class NotificationCostServiceMapper {

    public static PaperCostToInvalidate createPaperCostToInvalidateRequest(String reworkRecIndex, List<String> timelineElementsToInvalidate) {
        PaperCostToInvalidate paperCostToInvalidate = new PaperCostToInvalidate();
        paperCostToInvalidate.setRecIndex(reworkRecIndex);
        paperCostToInvalidate.setCostPhases(new ArrayList<>());
        timelineElementsToInvalidate.forEach(
                elementId -> {
                    if (elementId.contains(ATTEMPT_0)) {
                        paperCostToInvalidate.getCostPhases().add(AnalogUpdateCostPhase.SEND_ANALOG_DOMICILE_ATTEMPT_0);
                    } else if (elementId.contains(ATTEMPT_1)) {
                        paperCostToInvalidate.getCostPhases().add(AnalogUpdateCostPhase.SEND_ANALOG_DOMICILE_ATTEMPT_1);
                    }
                }
        );
        return paperCostToInvalidate;
    }
}
