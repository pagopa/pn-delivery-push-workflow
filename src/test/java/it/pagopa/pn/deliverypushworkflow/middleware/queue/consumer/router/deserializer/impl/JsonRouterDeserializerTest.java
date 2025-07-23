package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.router.deserializer.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypushworkflow.exceptions.PnEventRouterException;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

import java.util.Map;

import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ROUTER_DESERIALIZATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonRouterDeserializerTest {

    private final JsonRouterDeserializer deserializer = new JsonRouterDeserializer(new ObjectMapper());

    @Test
    void deserializeThrowsExceptionWhenPayloadIsNotString() {
        Message<Integer> message = mock(Message.class);
        when(message.getPayload()).thenReturn(123);

        PnEventRouterException exception = assertThrows(PnEventRouterException.class,
                () -> deserializer.deserialize(message, Object.class));
        assertEquals(ERROR_CODE_DELIVERYPUSH_ROUTER_DESERIALIZATION, exception.getProblem().getErrors().get(0).getCode());
    }

    @Test
    void deserializeThrowsExceptionWhenJsonProcessingFails() {
        Message<String> message = mock(Message.class);
        when(message.getPayload()).thenReturn("{invalidJson}");

        PnEventRouterException exception = assertThrows(PnEventRouterException.class,
                () -> deserializer.deserialize(message, Object.class));
        assertEquals(ERROR_CODE_DELIVERYPUSH_ROUTER_DESERIALIZATION, exception.getProblem().getErrors().get(0).getCode());
    }

    @Test
    void deserializeReturnsObjectWhenPayloadIsValidJson() {
        Message<String> message = mock(Message.class);
        when(message.getPayload()).thenReturn("{\"key\":\"value\"}");

        Map result = deserializer.deserialize(message, Map.class);
        assertEquals("value", result.get("key"));
    }
}