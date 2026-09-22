package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.resumepostpayment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.utils.HandleEventUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.List;

import static it.pagopa.pn.deliverypushworkflow.middleware.queue.utils.ChannelUtils.setMdc;

@Configuration
@RequiredArgsConstructor
@CustomLog
public class ResumePostPaymentConsumer {
    static final String PROCESS_NAME = "RESUME_POST_PAYMENT_CONSUMER";
    static final String VALIDATION_ERROR = "RESUME_POST_PAYMENT_ERROR";

    private final ObjectMapper objectMapper;
    private final ResumePostPaymentEventValidator validator;
    private final ResumePostPaymentHandler resumePostPaymentHandler;

    @SqsListener(queueNames = "#{@pnDeliveryPushWorkflowConfigs.topics.resumePostPayment}")
    public void consume(Message<String> message) {
        setMdc(message);
        log.logStartingProcess(PROCESS_NAME);

        try {
            final ResumePostPaymentEvent event;
            try {
                event = objectMapper.readValue(message.getPayload(), ResumePostPaymentEvent.class);
            } catch (JsonProcessingException exception) {
                log.error("{} invalid payload reason={}", VALIDATION_ERROR, exception.getOriginalMessage());
                log.logEndingProcess(PROCESS_NAME);
                return;
            }
            if (event == null) {
                log.error("{} invalid payload reason=null event", VALIDATION_ERROR);
                log.logEndingProcess(PROCESS_NAME);
                return;
            }

            List<String> validationErrors = validator.validate(event);
            if (!validationErrors.isEmpty()) {
                log.error("{} iun={} recIndex={} validationErrors={}", VALIDATION_ERROR,
                        event.getIun(), event.getRecIndex(), validationErrors);
                log.logEndingProcess(PROCESS_NAME);
                return;
            }

            HandleEventUtils.addIunAndRecIndexToMdc(event.getIun(), event.getRecIndex());
            resumePostPaymentHandler.handle(event);
            log.logEndingProcess(PROCESS_NAME);
        } catch (Exception exception) {
            log.logEndingProcess(PROCESS_NAME, false, exception.getMessage(), exception);
            HandleEventUtils.handleException(message.getHeaders(), exception);
            throw exception;
        }
    }
}