package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.ext;

import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypushworkflow.middleware.responsehandler.PaperChannelResponseHandler;
import org.springframework.stereotype.Component;

@Component
public class PrepareAnalogResponseEventHandler extends AbstractPaperChannelEventHandler {
    public PrepareAnalogResponseEventHandler(PaperChannelResponseHandler paperChannelResponseHandler) {
        super(paperChannelResponseHandler);
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.PREPARE_ANALOG_RESPONSE;
    }
}
