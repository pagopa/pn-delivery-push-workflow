package it.pagopa.pn.deliverypushworkflow.action.refinement;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.NotificationViewedCreationRequestDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.ScheduleRefinementDetailsInt;
import it.pagopa.pn.deliverypushworkflow.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefinementHandler {

    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationService notificationService;
    private final NotificationProcessCostService notificationProcessCostService;
    private final AttachmentUtils attachmentUtils;
    private final PnDeliveryPushWorkflowConfigs PnDeliveryPushWorkflowConfigs;


    public void handleRefinement(String iun, Integer recIndex) {

        log.info("Start HandleRefinement - iun {} id {}", iun, recIndex);
        boolean isNotificationViewed = timelineUtils.checkIsNotificationViewed(iun, recIndex);

        //Se la notifica è già stata visualizzata non viene perfezionata per decorrenza termini in quanto è già stata perfezionata per presa visione
        if( !isNotificationViewed ) {
            Instant refinementDate = retrieveRefinementDate(iun, recIndex);

            addRefinementElement(iun, recIndex, PnDeliveryPushWorkflowConfigs.getRetentionAttachmentDaysAfterRefinement(),true, refinementDate);
        } else {

            Instant viewedDate = retrieveViewedDate(iun, recIndex);

            Instant refinementDate = retrieveRefinementDate(iun, recIndex);

            //Se la notifica è già stata visualizzata ma in data precedente a quella del perfezionamento l'evento viene comunque generato
            if( refinementDate != null && viewedDate != null && viewedDate.isAfter(refinementDate) ) {
                addRefinementElement(iun, recIndex,null, false, refinementDate);
            } else {
                log.info("Notification is already viewed or paid, refinement will not start - iun={} id={}", iun, recIndex);
            }
        }
    }

    @Nullable
    private Instant retrieveViewedDate(String iun, Integer recIndex) {
        //FIND TIMELINE ELEMENT
        Instant viewedDate = timelineUtils.getNotificationViewCreationRequest(iun, recIndex).map(notificationViewCreationRequestTimelineElem -> {
            if(notificationViewCreationRequestTimelineElem.getDetails() instanceof NotificationViewedCreationRequestDetailsInt notificationViewedCreationRequestDetails) {
                return notificationViewedCreationRequestDetails.getEventTimestamp();
            }
            return null;
        }).orElse(null);

        log.info("Viewed date iun={} recIndex={} viewedDate={}", iun, recIndex, viewedDate);

        return viewedDate;
    }

    @Nullable
    private Instant retrieveRefinementDate(String iun, Integer recIndex) {
        // recupero la refinementDate dall'element dello schedule refinement
        Instant refinementDate = timelineUtils.getScheduleRefinement(iun, recIndex).map(scheduleRefinementTimelineElem -> {
            if(scheduleRefinementTimelineElem.getDetails() instanceof ScheduleRefinementDetailsInt scheduleRefinementTimelineElementDetails) {
                return scheduleRefinementTimelineElementDetails.getSchedulingDate();
            }
            return null;
        }).orElse(null);

        if (refinementDate == null)
            log.error("Schedule refinement not found iun={} recIndex={}", iun, recIndex);
        else
            log.info("Schedule refinement date iun={} recIndex={} refinementDate={}", iun, recIndex, refinementDate);

        return refinementDate;
    }

    private void addRefinementElement(String iun, Integer recIndex,  Integer attachmentRetention, Boolean addNotificationCost, Instant refinementDate) {
        log.info("Handle refinement - iun {} id {}", iun, recIndex);
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        MDCUtils.addMDCToContextAndExecute(
                notificationProcessCostService.getSendFeeAsync()
                        .doOnSuccess( notificationCost -> log.debug("Notification cost is {} - iun {} id {}",notificationCost, iun, recIndex))
                        .flatMap( res -> {
                            if(attachmentRetention != null){
                                return attachmentUtils.changeAttachmentsRetention(notification, attachmentRetention).collectList()
                                        .then(Mono.just(res));
                            }
                            return Mono.just(res);
                        })
                        .flatMap( notificationCost ->
                                Mono.fromCallable( () -> timelineUtils.buildRefinementTimelineElement(notification, recIndex, notificationCost, addNotificationCost, refinementDate))
                                        .flatMap( timelineElementInternal ->
                                                Mono.fromRunnable( () -> addTimelineElement(timelineElementInternal, notification))
                                                        .doOnSuccess( res -> log.info( "addTimelineElement OK {}", notification.getIun()))
                                        )
                        )
        ).block();
    }
    
    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
