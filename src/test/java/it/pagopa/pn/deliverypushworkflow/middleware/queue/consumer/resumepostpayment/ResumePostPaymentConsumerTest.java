package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.resumepostpayment;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ResumePostPaymentConsumerTest {

    @Mock
    private ResumePostPaymentHandler handler;
    @Mock
    private ObjectProvider<ResumePostPaymentHandler> handlerProvider;

    private ResumePostPaymentConsumer consumer;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(handlerProvider.getObject()).thenReturn(handler);
        consumer = new ResumePostPaymentConsumer(
                new ObjectMapper(),
                new ResumePostPaymentEventValidator(),
            handlerProvider
        );
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @ParameterizedTest
    @EnumSource(ResumeType.class)
    void consumeValidEventDelegatesExactlyOnce(ResumeType resumeType) {
        Message<String> message = message("""
                {"iun":"IUN_01","recIndex":0,"resumeType":"%s"}
                """.formatted(resumeType));

        consumer.consume(message);

        ResumePostPaymentEvent expectedEvent = ResumePostPaymentEvent.builder()
                .iun("IUN_01")
                .recIndex(0)
                .resumeType(resumeType)
                .build();
        Mockito.verify(handler, Mockito.times(1)).handle(expectedEvent);
        assertEquals("IUN_01", MDC.get(MDCUtils.MDC_PN_IUN_KEY));
        assertEquals("0", MDC.get(MDCUtils.MDC_PN_CTX_RECIPIENT_INDEX));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-json",
            "null",
            "{\"iun\":\"\",\"recIndex\":0,\"resumeType\":\"FIRST_ATTEMPT\"}",
            "{\"iun\":\"IUN_01\",\"resumeType\":\"FIRST_ATTEMPT\"}",
            "{\"iun\":\"IUN_01\",\"recIndex\":-1,\"resumeType\":\"FIRST_ATTEMPT\"}",
            "{\"iun\":\"IUN_01\",\"recIndex\":0}",
            "{\"iun\":\"IUN_01\",\"recIndex\":0,\"resumeType\":\"UNKNOWN\"}"
    })
    void consumeInvalidPayloadDoesNotDelegate(String payload) {
        assertDoesNotThrow(() -> consumer.consume(message(payload)));

        Mockito.verifyNoInteractions(handler);
    }

    @Test
    void consumeUsesResumePostPaymentQueueConfiguration() throws NoSuchMethodException {
        Method consumeMethod = ResumePostPaymentConsumer.class.getDeclaredMethod("consume", Message.class);
        SqsListener listener = consumeMethod.getAnnotation(SqsListener.class);

        assertNotNull(listener);
        assertArrayEquals(new String[]{"#{@pnDeliveryPushWorkflowConfigs.topics.resumePostPayment}"},
                listener.queueNames());
    }

    @Test
    void consumeTechnicalErrorIsPropagated() {
        Message<String> message = message("""
                {"iun":"IUN_01","recIndex":0,"resumeType":"FIRST_ATTEMPT"}
                """);
        RuntimeException technicalError = new RuntimeException("technical error");
        Mockito.doThrow(technicalError).when(handler).handle(Mockito.any());

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> consumer.consume(message));

        assertSame(technicalError, thrown);
        Mockito.verify(handler, Mockito.times(1)).handle(Mockito.any());
    }

    private Message<String> message(String payload) {
        return MessageBuilder.withPayload(payload)
                .setHeader("aws_messageId", "message-id")
                .build();
    }
}