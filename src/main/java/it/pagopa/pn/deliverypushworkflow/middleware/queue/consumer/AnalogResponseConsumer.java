package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.model.PaperChannelUpdate;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.paperchannel.PaperChannelSendClient;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypushworkflow.middleware.responsehandler.PaperChannelResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import static it.pagopa.pn.deliverypushworkflow.middleware.queue.utils.ChannelUtils.setMdc;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class AnalogResponseConsumer {

    private final PaperChannelResponseHandler paperChannelResponseHandler;

    @SqsListener(queueNames = "#{@pnDeliveryPushWorkflowConfigs.topics.analogResponseEvents}")
    public void analogResponseEventConsumer(Message<PaperChannelUpdate> message) {
        setMdc(message);
        try {
            PaperChannelUpdate paperChannelUpdate = message.getPayload();
            log.debug("Handle message from {} with payload {}", PaperChannelSendClient.CLIENT_NAME, paperChannelUpdate);
            paperChannelResponseHandler.paperChannelResponseReceiver(paperChannelUpdate);
        } catch (Exception ex) {
            HandleEventUtils.handleException(message.getHeaders(), ex);
            throw ex;
        }
    }
}
