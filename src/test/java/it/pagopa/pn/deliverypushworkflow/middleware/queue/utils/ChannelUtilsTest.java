package it.pagopa.pn.deliverypushworkflow.middleware.queue.utils;

import it.pagopa.pn.commons.utils.MDCUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import java.util.HashMap;

import static it.pagopa.pn.deliverypushworkflow.middleware.queue.utils.ChannelUtils.setMdc;
import static org.junit.jupiter.api.Assertions.*;


class ChannelUtilsTest {
   @Test
   void setMdc_setsAllMdcKeysWhenHeadersPresent() {
       HashMap<String, Object> headers = new HashMap<>();
       headers.put("aws_messageId", "aws-id-456");
       headers.put("X-Amzn-Trace-Id", "trace-xyz");
       headers.put("iun", "iun-abc");
       Message<String> message = new GenericMessage<>("payload", headers);

       setMdc(message);

       assertEquals("aws-id-456", MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
       assertEquals("trace-xyz", MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
       assertEquals("iun-abc", MDC.get(MDCUtils.MDC_PN_IUN_KEY));
   }

   @Test
   void setMdc_setsRandomTraceIdWhenHeaderMissing() {
       HashMap<String, Object> headers = new HashMap<>();
       headers.put("aws_messageId", "aws-id-789");
       Message<String> message = new GenericMessage<>("payload", headers);

       setMdc(message);

       assertEquals("aws-id-789", MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
       String traceId = MDC.get(MDCUtils.MDC_TRACE_ID_KEY);
       assertNotNull(traceId);
       assertFalse(traceId.isEmpty());
   }

   @Test
   void setMdc_doesNotSetIunWhenHeaderMissing() {
       HashMap<String, Object> headers = new HashMap<>();
       headers.put("aws_messageId", "aws-id-000");
       headers.put("X-Amzn-Trace-Id", "trace-000");
       Message<String> message = new GenericMessage<>("payload", headers);

       setMdc(message);

       assertEquals("aws-id-000", MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
       assertEquals("trace-000", MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
       assertNull(MDC.get(MDCUtils.MDC_PN_IUN_KEY));
   }

   @Test
   void setMdc_throwsExceptionOnNullMessage() {
       assertThrows(NullPointerException.class, () -> setMdc(null));
   }
}