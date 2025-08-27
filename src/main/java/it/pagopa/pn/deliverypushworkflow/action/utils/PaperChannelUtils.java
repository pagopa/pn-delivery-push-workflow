package it.pagopa.pn.deliverypushworkflow.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.paperchannel.AnalogDtoInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.paperchannel.CategorizedAttachmentsResultInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.EventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.*;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.model.SendResponse;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINENOTFOUND;

@Service
@Slf4j
public class PaperChannelUtils {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushConfigs;

    public PaperChannelUtils(TimelineService timelineService,
                             TimelineUtils timelineUtils,
                             PnDeliveryPushWorkflowConfigs pnDeliveryPushConfigs) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    public PhysicalAddressInt getSenderAddress(){
        return pnDeliveryPushConfigs.getPaperChannel().getSenderPhysicalAddress();
    }

    public String buildPrepareSimpleRegisteredLetterEventId(NotificationInt notification, Integer recIndex){
        return TimelineEventId.PREPARE_SIMPLE_REGISTERED_LETTER.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );
    }



    public String buildPrepareAnalogDomicileEventId(NotificationInt notification, Integer recIndex, int sentAttemptMade){
        return TimelineEventId.PREPARE_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .sentAttemptMade(sentAttemptMade)
                        .build()
        );
    }


    public String buildSendAnalogDomicileEventId(NotificationInt notification, Integer recIndex, int sentAttemptMade){
        return TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .sentAttemptMade(sentAttemptMade)
                        .build()
        );
    }




    public String buildSendAnalogFeedbackEventId(NotificationInt notification, Integer recIndex, int sentAttemptMade){
        return TimelineEventId.SEND_ANALOG_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .sentAttemptMade(sentAttemptMade)
                        .build()
        );
    }

    public String addPrepareSimpleRegisteredLetterToTimeline(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex,
                                                           String eventId) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildPrepareSimpleRegisteredLetterTimelineElement(recIndex, notification, physicalAddress, eventId);
        addTimelineElement(timelineElementInternal,
                notification
        );
        return timelineElementInternal.getElementId();
    }

    public String addSendSimpleRegisteredLetterToTimeline(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex,
                                                          SendResponse sendResponse, String productType, String prepareRequestId,
                                                          List<String> replacedF24AttachmentUrls, CategorizedAttachmentsResultInt categorizedAttachmentsResult) {
        TimelineElementInternal timelineElementInternal = timelineUtils. buildSendSimpleRegisteredLetterTimelineElement(
                recIndex, notification, physicalAddress, sendResponse, productType, prepareRequestId,replacedF24AttachmentUrls, categorizedAttachmentsResult);
        addTimelineElement(timelineElementInternal,
                notification
        );
        return timelineElementInternal.getElementId();
    }


    public String addPrepareAnalogNotificationToTimeline(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex, String relatedRequestId,
                                                    int sentAttemptMade, String eventId, PhysicalAddressInt discoveredAddress) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildPrepareAnalogNotificationTimelineElement(physicalAddress, recIndex, notification, relatedRequestId, sentAttemptMade, eventId, discoveredAddress);
        addTimelineElement(timelineElementInternal,
                notification
        );
        return timelineElementInternal.getElementId();
    }


    public void addPrepareAnalogFailureTimelineElement(PhysicalAddressInt foundAddress, String prepareRequestId, String failureCause, Integer recIndex, NotificationInt notification) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildPrepareAnalogFailureTimelineElement(foundAddress, prepareRequestId, failureCause, recIndex, notification);
        addTimelineElement(timelineElementInternal,
                notification
        );
    }

    
    public String addSendAnalogNotificationToTimeline(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex,
                                                      AnalogDtoInt analogDtoInfo, List<String> replacedF24AttachmentUrls,
                                                      CategorizedAttachmentsResultInt categorizedAttachmentsResult) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildSendAnalogNotificationTimelineElement(
                physicalAddress, recIndex, notification, analogDtoInfo, replacedF24AttachmentUrls, categorizedAttachmentsResult);
        addTimelineElement(timelineElementInternal,
                notification
        );
        return timelineElementInternal.getElementId();
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

    public TimelineElementInternal getPaperChannelNotificationTimelineElement(String iun, String eventId) {
        //Viene ottenuto l'oggetto di timeline
        Optional<TimelineElementInternal> timelineElement = timelineService.getTimelineElement(iun, eventId);

        if (timelineElement.isPresent()) {
            return timelineElement.get();
        } else {
            log.error("There isn't timelineElement - iun {} eventId {}", iun, eventId);
            throw new PnInternalException("There isn't timelineElement - iun " + iun + " eventId " + eventId, ERROR_CODE_DELIVERYPUSH_TIMELINENOTFOUND);
        }
    }

    public TimelineElementInternal getSendRequestElementByPrepareRequestId(String iun, String prepareRequestId) {
        Set<TimelineElementInternal> timeline = timelineService.getTimeline(iun, true);
        Optional<TimelineElementInternal> sendRequestIdOpt =  timeline.stream()
                .filter(timelineElement -> filterSendByPrepareRequestId(timelineElement, prepareRequestId))
                .findFirst();
        
        if(sendRequestIdOpt.isPresent()){
            return sendRequestIdOpt.get();
        }else {
            log.warn("SendRequestId is not present for iun={} prepareRequestId={}", iun, prepareRequestId);
            return null;
        }
    }

    private boolean filterSendByPrepareRequestId(TimelineElementInternal el, String prepareRequestId) {
        switch(el.getCategory()) {
            case SEND_SIMPLE_REGISTERED_LETTER -> {
                SimpleRegisteredLetterDetailsInt details = (SimpleRegisteredLetterDetailsInt) el.getDetails();
                return prepareRequestId.equals(details.getPrepareRequestId());
            }
            case SEND_ANALOG_DOMICILE -> {
                SendAnalogDetailsInt details = (SendAnalogDetailsInt) el.getDetails();
                return prepareRequestId.equals(details.getPrepareRequestId());
            }
            default -> { return false; }

        }
    }

}
