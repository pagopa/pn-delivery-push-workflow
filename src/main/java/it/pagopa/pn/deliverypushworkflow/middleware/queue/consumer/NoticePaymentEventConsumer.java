package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.api.dto.events.PnNoticePaymentEvent;
import it.pagopa.pn.api.dto.events.PnNoticePaymentPaidEvent;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.cost.NoticePaymentInt;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.nationalregistries.NationalRegistriesClient;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypushworkflow.middleware.responsehandler.NoticePaymentEventHandler;
import it.pagopa.pn.deliverypushworkflow.service.mapper.NoticePaymentMapper;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import static it.pagopa.pn.deliverypushworkflow.middleware.queue.utils.ChannelUtils.setMdc;

@Configuration
@AllArgsConstructor
@CustomLog
public class NoticePaymentEventConsumer {
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;
    private final NoticePaymentEventHandler noticePaymentEventHandler;

    @SqsListener(queueNames = "#{@pnDeliveryPushWorkflowConfigs.topics.noticePayment}")
    public void noticePaymentEventConsumer(Message<PnNoticePaymentPaidEvent> message) {
        setMdc(message);
        try {
            log.info("Handle message from {} with content {}", NationalRegistriesClient.CLIENT_NAME, message);

            NoticePaymentInt response = NoticePaymentMapper.messageToInternal(message.getPayload());
            noticePaymentEventHandler.handleResponse(response);

        } catch (Exception ex) {
            HandleEventUtils.handleException(message.getHeaders(), ex);
            throw ex;
        }
    }
}
