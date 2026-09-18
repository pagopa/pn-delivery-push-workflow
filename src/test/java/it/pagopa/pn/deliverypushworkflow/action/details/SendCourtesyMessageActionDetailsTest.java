package it.pagopa.pn.deliverypushworkflow.action.details;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.DeliveryModeInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.List;

class SendCourtesyMessageActionDetailsTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new Jackson2ObjectMapperBuilder().build();
    }

    @Test
    void detailsWithoutPlannedAddressIdKeepTheFieldNull() throws Exception {
        String payload = "{\"actionType\":\"SEND_COURTESY_MESSAGE_ACTION\",\"channel\":\"EMAIL\",\"retryIndex\":0,\"deliveryMode\":\"DIGITAL\",\"plannedChannels\":[\"EMAIL\"]}";

        SendCourtesyMessageActionDetails details = objectMapper.readValue(payload, SendCourtesyMessageActionDetails.class);

        Assertions.assertNull(details.getPlannedAddressId());
        Assertions.assertEquals(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL, details.getChannel());
    }

    @Test
    void unknownPropertiesAreIgnoredOnDeserialization() throws Exception {
        String payload = "{\"actionType\":\"SEND_COURTESY_MESSAGE_ACTION\",\"channel\":\"EMAIL\",\"retryIndex\":0,\"deliveryMode\":\"DIGITAL\",\"plannedChannels\":[\"EMAIL\"],"
                + "\"plannedAddressId\":\"COURTESY_PLANNED#iun_01#0#EMAIL\",\"aFieldAddedByALaterVersion\":\"value\"}";

        SendCourtesyMessageActionDetails details = objectMapper.readValue(payload, SendCourtesyMessageActionDetails.class);

        Assertions.assertEquals("COURTESY_PLANNED#iun_01#0#EMAIL", details.getPlannedAddressId());
    }

    @Test
    void toBuilderKeepsEveryFieldButTheOverriddenOne() {
        SendCourtesyMessageActionDetails details = SendCourtesyMessageActionDetails.builder()
                .channel(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS)
                .retryIndex(0)
                .deliveryMode(DeliveryModeInt.ANALOG)
                .plannedChannels(List.of(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS))
                .plannedAddressId("COURTESY_PLANNED#iun_01#0#SMS")
                .build();

        SendCourtesyMessageActionDetails next = details.toBuilder().retryIndex(1).build();

        Assertions.assertEquals(1, next.getRetryIndex());
        Assertions.assertEquals(details.getChannel(), next.getChannel());
        Assertions.assertEquals(details.getDeliveryMode(), next.getDeliveryMode());
        Assertions.assertEquals(details.getPlannedChannels(), next.getPlannedChannels());
        Assertions.assertEquals(details.getPlannedAddressId(), next.getPlannedAddressId());
    }
}
