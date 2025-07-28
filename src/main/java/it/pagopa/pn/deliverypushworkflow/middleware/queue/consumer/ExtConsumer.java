package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.externalchannel.ExternalChannelSendClient;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.router.EventRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

import static it.pagopa.pn.deliverypushworkflow.middleware.queue.utils.ChannelUtils.setMdc;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class ExtConsumer {
    private static final String DEFAULT_EVENT_TYPE = "SEND_PEC_RESPONSE";
    private static final String MOCK_EXT_CHANNEL_EVENT_TYPE = "EXTERNAL_CHANNELS_EVENT";
    private final EventRouter eventRouter;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;

    @SqsListener(queueNames = "#{@pnDeliveryPushWorkflowConfigs.topics.fromExternalChannel}")
    public void pnExtChannelEventInboundConsumer(Message<String> message) {
        setMdc(message);
        try {
            log.info("Handle message from {} with content {}", ExternalChannelSendClient.CLIENT_NAME, message);
            String eventType = extractEventType(message);

            EventRouter.RoutingConfig routingConfig = EventRouter.RoutingConfig.builder()
                    .eventType(eventType)
                    .deserializePayload(true)
                    .build();

            eventRouter.route(message, routingConfig);
        } catch (Exception ex) {
            HandleEventUtils.handleException(message.getHeaders(), ex);
            throw ex;
        }
    }

    private String extractEventType(Message<?> message) {
        String headerEventType = (String) message.getHeaders().get("eventType");
        /*
         * Se l'header eventType non è presente, viene utilizzato un event type prestabilito.
         * Ereditiamo questo comportamento dalla vecchia modalità di gestione degli eventi relativi a questa coda.
         * In cui potevano arrivare 2 tipologie di eventi con eventType specificato, o in alternativa
         * un evento senza eventType, e in questo caso lo indirizzavamo noi assegnandogli un eventType specifico (SEND_PEC_RESPONSE).
         */
        if(!StringUtils.hasText(headerEventType) || headerEventType.equals(MOCK_EXT_CHANNEL_EVENT_TYPE)) {
            log.info("Event type not specified in message headers, using default event type: {}", DEFAULT_EVENT_TYPE);
            return DEFAULT_EVENT_TYPE;
        }

        return headerEventType;
    }
}
