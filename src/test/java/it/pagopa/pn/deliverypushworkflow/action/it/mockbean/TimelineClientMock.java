package it.pagopa.pn.deliverypushworkflow.action.it.mockbean;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.MethodExecutor;
import it.pagopa.pn.deliverypushworkflow.action.notificationview.NotificationViewedRequestHandler;
import it.pagopa.pn.deliverypushworkflow.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notificationviewed.NotificationViewedInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.EventId;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NewTimelineElement;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.TimelineCategory;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.TimelineElement;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.TimelineElementDetails;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.timeline.TimelineClient;
import it.pagopa.pn.deliverypushworkflow.service.NotificationCancellationService;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId.NOTIFICATION_CANCELLATION_REQUEST;

@Slf4j
public class TimelineClientMock implements TimelineClient {
    public static final String SIMULATE_VIEW_NOTIFICATION= "simulate-view-notification";
    public static final String SIMULATE_CANCEL_NOTIFICATION= "simulate-cancel-notification";
    public static final String SIMULATE_AFTER_CANCEL_NOTIFICATION= "simulate-after-cancel-notification";
    public static final String SIMULATE_RECIPIENT_WAIT = "simulate-recipient-wait";
    public static final String WAIT_SEPARATOR = "@@";

    private final NotificationViewedRequestHandler notificationViewedRequestHandler;
    private CopyOnWriteArrayList<TimelineElement> timelineList;
    final HashMap<String, Long> counter = new HashMap<>();
    private final NotificationService notificationService;
    private final NotificationUtils notificationUtils;
    private final NotificationCancellationService notificationCancellationService;

    public TimelineClientMock(@Lazy NotificationViewedRequestHandler notificationViewedRequestHandler, @Lazy NotificationService notificationService,
                              @Lazy NotificationUtils notificationUtils, @Lazy NotificationCancellationService notificationCancellationService) {
        this.notificationViewedRequestHandler = notificationViewedRequestHandler;
        timelineList = new CopyOnWriteArrayList<>();
        this.notificationService = notificationService;
        this.notificationUtils = notificationUtils;
        this.notificationCancellationService = notificationCancellationService;
    }

    public void clear() {
        this.timelineList = new CopyOnWriteArrayList<>();
        this.counter.clear();
    }


    private void checkAndAddTimelineElement(NewTimelineElement newTimelineElement) {
        log.debug("[TEST] Start checkAndAddTimelineElement {}", newTimelineElement);
        TimelineElement dto = newTimelineElement.getTimelineElement();

        if(dto.getDetails().getRecIndex() != null){

            log.debug("[TEST] Ok details is present {}", dto);

            NotificationRecipientInt notificationRecipientInt = getRecipientInt(dto);
            String simulateViewNotificationString = SIMULATE_VIEW_NOTIFICATION + dto.getElementId();
            String simulateRecipientWaitString = SIMULATE_RECIPIENT_WAIT + dto.getElementId();
            String simulateCancelNotificationString = SIMULATE_CANCEL_NOTIFICATION + dto.getElementId();

            if(notificationRecipientInt.getTaxId().startsWith(simulateViewNotificationString)){
                log.debug("[TEST] Simulate view notification {}", dto);
                //Viene simulata la visualizzazione della notifica prima di uno specifico inserimento in timeline
                NotificationViewedInt notificationViewedInt = NotificationViewedInt.builder()
                        .iun(dto.getIun())
                        .recipientIndex(dto.getDetails().getRecIndex())
                        .viewedDate(Instant.now())
                        .build();
                notificationViewedRequestHandler.handleViewNotificationDelivery(notificationViewedInt);
            }else if(notificationRecipientInt.getTaxId().startsWith(simulateRecipientWaitString)){
                //Viene simulata l'attesa in un determinato stato (elemento di timeline) per uno specifico recipient.
                // L'attesa dura fino all'inserimento in timeline di un determinato elemento per un altro recipient
                String waitForElementId = notificationRecipientInt.getTaxId().replaceFirst(".*" + WAIT_SEPARATOR, "");
                log.debug("[TEST] Wait for elementId {}", waitForElementId);

                MethodExecutor.waitForExecution(
                        () -> Optional.ofNullable(getTimelineElement(dto.getIun(), waitForElementId, false))
                );

            }else if(notificationRecipientInt.getTaxId().startsWith(simulateCancelNotificationString)){
                log.debug("[TEST] Simulate cancel notification {}", dto);
                simulateCancellation(dto);
            }
        }

        log.debug("[TEST] Add timeline element {}", dto);

/*        if(getTimelineElement(dto.getIun(), dto.getElementId()).isPresent()){
            log.error("TimelineElement is already present timelineElementId={}",dto.getElementId());
            throw new RuntimeException("TimelineElement is already present");
        }*/

        timelineList.add(dto);


        if( dto.getDetails() != null && dto.getDetails().getRecIndex() != null) {

            NotificationRecipientInt notificationRecipientInt = getRecipientInt(dto);
            String simulateAfterCancelNotificationString = dto.getElementId() + SIMULATE_AFTER_CANCEL_NOTIFICATION ;

            if (notificationRecipientInt.getTaxId().endsWith(simulateAfterCancelNotificationString)) {
                //Viene simulata la cancellazione della notifica DOPO di uno specifico inserimento in timeline
                log.debug("[TEST] Simulate after cancel notification {}", dto);
                simulateCancellation(dto);
            }
        }
    }

    private void simulateCancellation(TimelineElement dto) {
        //Viene simulata la cancellazione della notifica prima di uno specifico inserimento in timeline

        //Popolo in anticipo la timeline con un elemento di cancellazione scatenato da flusso HTTP (Che in questo dominio non è implemenato)
        timelineList.add(buildMockedCancellationRequest(dto.getIun()));

        // Parto dal secondo step di cancellazione
        notificationCancellationService.continueCancellationProcess( dto.getIun() );
    }

    private TimelineElement buildMockedCancellationRequest(String iun) {
        String elementId = NOTIFICATION_CANCELLATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        TimelineElementDetails details = new TimelineElementDetails();
        details.setCancellationRequestId("cancellation-request-id-" + iun);

        TimelineElement timelineElement = new TimelineElement();
        timelineElement.setIun(iun);
        timelineElement.setElementId(elementId);
        timelineElement.setCategory(TimelineCategory.NOTIFICATION_CANCELLATION_REQUEST);
        timelineElement.setDetails(details);
        timelineElement.setLegalFactsIds(Collections.emptyList());
        timelineElement.setTimestamp(Instant.now());
        return timelineElement;
    }

    private NotificationRecipientInt getRecipientInt(TimelineElement row) {
        if(row.getDetails().getRecIndex() != null){
            NotificationInt notificationInt = this.notificationService.getNotificationByIun(row.getIun());
            return notificationUtils.getRecipientFromIndex(notificationInt, row.getDetails().getRecIndex());
        }else {
            throw new PnInternalException("There isn't recipient index for timeline element", "test");
        }
    }

    @Override
    public boolean addTimelineElement(NewTimelineElement newTimelineElement) {
        checkAndAddTimelineElement(newTimelineElement);
        return false;
    }

    @Override
    public Long retrieveAndIncrementCounterForTimelineEvent(String timelineId) {
        Long v = 0L;
        synchronized (counter){
            if (counter.containsKey(timelineId))
            {
                v = counter.get(timelineId);
            }
            v = v+1;
            counter.put(timelineId, v);
        }
        return v;
    }

    @Override
    public TimelineElement getTimelineElement(String iun, String timelineId, Boolean strongly) {
        log.debug("[TEST] Start getTimelineElement iun={} timelineId={} in timelineIds={}", iun, timelineId, timelineList.stream().map(TimelineElement::getElementId).toList());
        return timelineList.stream().filter(timelineElement -> timelineId.equals(timelineElement.getElementId()) && iun.equals(timelineElement.getIun())).findFirst().orElse(null);
    }

    @Override
    public TimelineElementDetails getTimelineElementDetails(String iun, String timelineId) {
        TimelineElement timelineElement = getTimelineElement(iun, timelineId, false);
        if (timelineElement != null) {
            return timelineElement.getDetails();
        } else {
            log.debug("[TEST] Timeline element not found for iun={} and timelineId={}", iun, timelineId);
            return null;
        }
    }

    @Override
    public TimelineElementDetails getTimelineElementDetailForSpecificRecipient(String iun, Integer recIndex, Boolean confidentialInfoRequired, TimelineCategory category) {
        TimelineElement timelineElement = getTimelineElementForSpecificRecipient(iun, recIndex, category);
        if (timelineElement != null) {
            return timelineElement.getDetails();
        } else {
            log.debug("[TEST] Timeline element not found for iun={}, recIndex={}, category={}", iun, recIndex, category);
            return null;
        }
    }

    @Override
    public TimelineElement getTimelineElementForSpecificRecipient(String iun, Integer recIndex, TimelineCategory category) {
        return timelineList.stream()
                .filter(timelineElement -> iun.equals(timelineElement.getIun()) &&
                        timelineElement.getCategory() == category &&
                        timelineElement.getDetails().getRecIndex().equals(recIndex)
                ).findFirst()
                .orElse(null);
    }

    @Override
    public List<TimelineElement> getTimeline(String iun, Boolean confidentialInfoRequired, Boolean strongly, String timelineId) {
        return timelineList.stream()
                .filter(timelineElement ->
                    iun.equals(timelineElement.getIun()) &&
                    (timelineId == null || timelineElement.getElementId().startsWith(timelineId))
                )
                .toList();
    }
}
