package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.nationalregistries.NationalRegistriesClient;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypushworkflow.middleware.responsehandler.NationalRegistriesResponseHandler;
import it.pagopa.pn.deliverypushworkflow.utils.NationalRegistriesMessageUtil;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.nationalregistries.model.AddressSQSMessage;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.nationalregistries.model.AddressSQSMessageDigitalAddressInner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.List;

import static it.pagopa.pn.deliverypushworkflow.middleware.queue.utils.ChannelUtils.setMdc;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class NationalRegistriesConsumer {
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;
    private final NationalRegistriesResponseHandler nationalRegistriesResponseHandler;

    @SqsListener(queueNames = "#{@pnDeliveryPushWorkflowConfigs.topics.nationalRegistriesEvents}")
    public void pnNationalRegistriesEventInboundConsumer(Message<AddressSQSMessage> message) {
        setMdc(message);
        try {
            log.info("Handle message from {} with content {}", NationalRegistriesClient.CLIENT_NAME, message);

            List<AddressSQSMessageDigitalAddressInner> digitalAddresses = message.getPayload().getDigitalAddress();
            String correlationId = message.getPayload().getCorrelationId();
            NationalRegistriesResponse response = NationalRegistriesMessageUtil.buildPublicRegistryResponse(correlationId, digitalAddresses);
            nationalRegistriesResponseHandler.handleResponse(response);

        } catch (Exception ex) {
            HandleEventUtils.handleException(message.getHeaders(), ex);
            throw ex;
        }
    }
}
