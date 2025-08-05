package it.pagopa.pn.deliverypushworkflow.service.mapper;

import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.NotificationCostResponseInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.NotificationCostResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NotificationCostResponseMapperTest {

    @Test
    void externalToInternal() {
        NotificationCostResponseInt actual = NotificationCostResponseMapper.externalToInternal(buildNotificationCostResponse());

        Assertions.assertEquals(buildNotificationCostResponseInt(), actual);

    }

    private NotificationCostResponse buildNotificationCostResponse() {
        NotificationCostResponse response = new NotificationCostResponse();
        response.setIun("001");
        response.setRecipientIdx(2);
        return response;
    }

    private NotificationCostResponseInt buildNotificationCostResponseInt() {
        return NotificationCostResponseInt.builder()
                .iun("001")
                .recipientIdx(2)
                .build();
    }
}