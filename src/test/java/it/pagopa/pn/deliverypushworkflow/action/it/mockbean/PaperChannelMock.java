package it.pagopa.pn.deliverypushworkflow.action.it.mockbean;

import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventIdBuilder;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.model.*;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.paperchannel.PaperChannelPrepareRequest;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.paperchannel.PaperChannelSendClient;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypushworkflow.middleware.responsehandler.PaperChannelResponseHandler;
import it.pagopa.pn.deliverypushworkflow.utils.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PaperChannelMock implements PaperChannelSendClient {

    //ANALOG
    public static final String EXTCHANNEL_SEND_SUCCESS = "OK"; //Invio notifica ok
    public static final String EXTCHANNEL_SEND_FAIL = "FAIL"; //Invio notifica fallita
    public static final String EXTCHANNEL_SEND_FAIL_KOUNREACHABLE = "KOUNREACHABLE"; //Invio notifica fallita
    public static final String EXT_CHANNEL_SEND_NEW_ADDR = "NEW_ADDR:"; //Invio notifica fallita con nuovo indirizzo da investigazione
    //Esempio: La combinazione di EXT_CHANNEL_SEND_NEW_ADDR + EXTCHANNEL_SEND_OK ad esempio significa -> Invio notifica fallito ma con nuovo indirizzo trovato e l'invio a tale indirizzo avrà successo
    public static final String EXTCHANNEL_SEND_DECEASED = "DECEASED"; //Invio notifica ok ma con destinatario deceduto
    public static final int EXTCHANNEL_SECOND_SEND_ATTEMPT = 1;
    public static final String EXTCHANNEL_SEND_PRODUCT_RIR = "PRODUCT_RIR"; //Invio notifica con prodotto RIR
    public static final String EXTCHANNEL_RIR_NO_FEEDBACK = EXTCHANNEL_SEND_PRODUCT_RIR + "_NO_FEEDBACK"; //Invio notifica con prodotto RIR che non riceve esito
    public static final String EXTCHANNEL_RIR_WITH_FEEDBACK_OK = EXTCHANNEL_SEND_PRODUCT_RIR + "_FEEDBACK_OK"; //Invio notifica con prodotto RIR che produce un esito OK
    public static final String EXTCHANNEL_SEND_PRODUCT_RIR_ATTEMPT_RELATED = "PRODUCT_RIR_ATTEMPT_RELATED";
    public static final String EXTCHANNEL_RIR_WITH_PREPARE_KO_SECOND_ATTEMPT = EXTCHANNEL_SEND_PRODUCT_RIR_ATTEMPT_RELATED + "_PREPARE_KO_SECOND_ATTEMPT"; //Invio notifica con prodotto RIR che produce un esito KO al secondo tentativo di prepare


    public static final int WAITING_TIME = 3000;
    private static final Pattern NEW_ADDRESS_INPUT_PATTERN = Pattern.compile("^" + EXT_CHANNEL_SEND_NEW_ADDR + "(.*)$");
    public static final String PAPER_ADDRESS_FULL_NAME = "full name";
    public static final String PAPER_ADDRESS_CITTA = "citta";
    public static final String PAPER_ADDRESS_ITALY = "italy";

    private final PaperChannelResponseHandler paperChannelResponseHandler;

    public PaperChannelMock(@Lazy PaperChannelResponseHandler paperChannelResponseHandler) {
        this.paperChannelResponseHandler = paperChannelResponseHandler;
    }

    @Override
    public void prepare(PaperChannelPrepareRequest paperChannelPrepareRequest) {
        log.info("[TEST] prepare paperChannelPrepareRequest:{}", paperChannelPrepareRequest);

        ThreadPool.start(new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Assertions.assertDoesNotThrow(() -> {
                String address = paperChannelPrepareRequest.getDiscoveredAddress()!=null?paperChannelPrepareRequest.getDiscoveredAddress().getAddress():null;
                if (address == null)
                    address = paperChannelPrepareRequest.getPaAddress()!=null?paperChannelPrepareRequest.getPaAddress().getAddress():null;
                simulatePrepareResponse(paperChannelPrepareRequest.getRequestId(), address);
            });

        }));

    }

    @Override
    public SendResponse send(PaperChannelSendRequest paperChannelSendRequest) {
        log.info("[TEST] send paperChannelSendRequest:{}", paperChannelSendRequest);


        ThreadPool.start(new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Assertions.assertDoesNotThrow(() -> simulateSendResponse(paperChannelSendRequest.getRequestId(), paperChannelSendRequest.getReceiverAddress().getAddress()));

        }));

        return new SendResponse()
                .amount(100)
                .numberOfPages(1)
                .envelopeWeight(100);
    }


    private void simulatePrepareResponse(String timelineEventId,  String address) {
        PaperChannelUpdate singleStatusUpdate = new PaperChannelUpdate();
        PrepareEvent prepareEvent = new PrepareEvent();
        prepareEvent.setStatusDateTime(Instant.now());
        prepareEvent.setRequestId(timelineEventId);
        prepareEvent.setProductType("NR_AR");

        int sendAttempt = extractAttemptFromTimelineId(timelineEventId);

        String status;
        if (address == null) {
            status = "KOUNREACHABLE";
        } else {
            Matcher matcher = NEW_ADDRESS_INPUT_PATTERN.matcher(address);
            if (matcher.find()) {
                status = "OK";
            } else if (address.startsWith(EXTCHANNEL_SEND_FAIL_KOUNREACHABLE)) {
                status = "KOUNREACHABLE";
            } else if (address.startsWith(EXTCHANNEL_SEND_SUCCESS)) {
                status = "OK";
            } else if (address.startsWith(EXTCHANNEL_SEND_FAIL)) {
                status = "OK";
            } else if (address.startsWith(EXTCHANNEL_SEND_DECEASED)) {
                status = "OK";
            } else if (address.startsWith(EXTCHANNEL_RIR_WITH_PREPARE_KO_SECOND_ATTEMPT) && sendAttempt == EXTCHANNEL_SECOND_SEND_ATTEMPT) {
                status = "KO";
                prepareEvent.setProductType("RIR");
                prepareEvent.setFailureDetailCode(FailureDetailCodeEnum.D00);
            } else if (address.startsWith(EXTCHANNEL_SEND_PRODUCT_RIR)) {
                status = "OK";
                prepareEvent.setProductType("RIR");
            } else {
                throw new IllegalArgumentException("Address " + address + " do not match test rule for mocks");
            }
        }

        if (status.equals("OK")) {
            prepareEvent.setReceiverAddress(new AnalogAddress());
            Objects.requireNonNull(prepareEvent.getReceiverAddress()).setFullname(PAPER_ADDRESS_FULL_NAME);
            prepareEvent.getReceiverAddress().setAddress(address);
            prepareEvent.getReceiverAddress().setCity(PAPER_ADDRESS_CITTA);
            prepareEvent.getReceiverAddress().setCountry(PAPER_ADDRESS_ITALY);
        }

        singleStatusUpdate.setPrepareEvent(prepareEvent);
        prepareEvent.setStatusCode(StatusCodeEnum.valueOf(status));

        Assertions.assertNotNull(status);

        paperChannelResponseHandler.paperChannelResponseReceiver(singleStatusUpdate);
    }

    private int extractAttemptFromTimelineId(String timelineId) {
        //<timelineId = CATEGORY_VALUE>;IUN_<IUN_VALUE>;RECINDEX_<RECINDEX_VALUE>;ATTEMPT_<ATTEMPT_VALUE>...
        String sendAttemptString = timelineId.split("\\" + TimelineEventIdBuilder.DELIMITER)[3].replace("ATTEMPT_", "");
        return Integer.parseInt(sendAttemptString);
    }

    private void simulateSendResponse(String timelineEventId, String address) throws InterruptedException {
        PaperChannelUpdate singleStatusUpdate = new PaperChannelUpdate();
        SendEvent sendEvent = new SendEvent();
        sendEvent.setStatusDateTime(Instant.now());
        sendEvent.setRequestId(timelineEventId);

        String newAddress;
        StatusCodeEnum status;
        Matcher matcher = NEW_ADDRESS_INPUT_PATTERN.matcher(address);
        if (matcher.find()) {
            status = StatusCodeEnum.KO;
            newAddress = matcher.group(1).trim();
        } else if (address.startsWith(EXTCHANNEL_SEND_FAIL)) {
            status = StatusCodeEnum.KO;
            sendEvent.setStatusDetail("errore fail mock!");
            newAddress = null;
        } else if (address.startsWith(EXTCHANNEL_SEND_SUCCESS)) {
            status = StatusCodeEnum.OK;
            newAddress = null;
        } else if(address.startsWith(EXTCHANNEL_SEND_DECEASED)) {
            status = StatusCodeEnum.OK;
            newAddress = null;
            sendEvent.setDeliveryFailureCause("M02");
            Thread.sleep(WAITING_TIME);
        } else if(address.startsWith(EXTCHANNEL_RIR_NO_FEEDBACK) || address.startsWith(EXTCHANNEL_RIR_WITH_PREPARE_KO_SECOND_ATTEMPT)) {
            Thread.sleep(WAITING_TIME);
            // In questo caso non dobbiamo restituire un feedback, quindi facciamo terminare il metodo.
            return;
        } else if(address.startsWith(EXTCHANNEL_RIR_WITH_FEEDBACK_OK)) {
            status = StatusCodeEnum.OK;
            newAddress = null;
        } else {
            throw new IllegalArgumentException("Address " + address + " do not match test rule for mocks");
        }


        if (newAddress != null) {
            sendEvent.setDiscoveredAddress(new AnalogAddress());
            Objects.requireNonNull(sendEvent.getDiscoveredAddress()).setFullname(PAPER_ADDRESS_FULL_NAME);
            sendEvent.getDiscoveredAddress().setAddress(newAddress);
            sendEvent.getDiscoveredAddress().setCity(PAPER_ADDRESS_CITTA);
            sendEvent.getDiscoveredAddress().setCountry(PAPER_ADDRESS_ITALY);
        }

        sendEvent.setStatusCode(status);


        singleStatusUpdate.setSendEvent(sendEvent);

        paperChannelResponseHandler.paperChannelResponseReceiver(singleStatusUpdate);
    }

}
