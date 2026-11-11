package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.api.dto.events.PnNoticePaymentPaidEvent;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.cost.NoticePaymentInt;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.nationalregistries.NationalRegistriesClient;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypushworkflow.middleware.responsehandler.NoticePaymentEventHandler;
import it.pagopa.pn.deliverypushworkflow.middleware.responsehandler.NoticePaymentPaidEventValidator;
import it.pagopa.pn.deliverypushworkflow.service.mapper.NoticePaymentMapper;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.List;

import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALIDEVENTCODE;
import static it.pagopa.pn.deliverypushworkflow.middleware.queue.utils.ChannelUtils.setMdc;

@Configuration
@AllArgsConstructor
@CustomLog
public class NoticePaymentEventConsumer {
    static final String PROCESS_NAME = "NOTICE_PAYMENT_PAID_CONSUMER";
    static final String VALIDATION_ERROR = "NOTICE_PAYMENT_PAID_ERROR";

    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;
    private final NoticePaymentPaidEventValidator validator;
    private final NoticePaymentEventHandler noticePaymentEventHandler;

    @SqsListener(queueNames = "#{@pnDeliveryPushWorkflowConfigs.topics.noticePayment}")
    public void noticePaymentEventConsumer(Message<PnNoticePaymentPaidEvent.Payload> message) {
        setMdc(message);
        try {
            log.info("Handle message from {} with content {}", NationalRegistriesClient.CLIENT_NAME, message);
            List<String> validationErrors = validator.validate(message.getPayload());
            if (!validationErrors.isEmpty()) {
                log.error("{} validationErrors={}", VALIDATION_ERROR, validationErrors);
                log.logEndingProcess(PROCESS_NAME);
                throw new PnInternalException("Validation errors: " + validationErrors, ERROR_CODE_DELIVERYPUSH_INVALIDEVENTCODE);
            }
            NoticePaymentInt response = NoticePaymentMapper.messageToInternal(message.getPayload());
            HandleEventUtils.addIunToMdc(response.getIun());
            noticePaymentEventHandler.handleResponse(response);

        } catch (Exception ex) {
            HandleEventUtils.handleException(message.getHeaders(), ex);
            throw ex;
        }
    }
}
