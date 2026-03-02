package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.AddTimelineElementResponse;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.timeline.TimelineClient;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_TIMELINESERVICE_TIMELINE_ELEMENT_NOT_PRESENT;

@Service
@Slf4j
@RequiredArgsConstructor
public class TimelineServiceHttpImpl implements TimelineService {

    private final TimelineClient timelineClient;

    @Override
    public AddTimelineElementResponse addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        log.info("addTimelineElement - IUN={} and timelineId={}", element.getIun(), element.getElementId());
        AddTimelineElementResponse addTimelineElementResponse = timelineClient.addTimelineElement(element, notification);
        //dobbiamo sovrascrivere il timelineElementId del TimelineElementInternal con quello generato da timeline-service in quanto la gestione del
        //suffisso REWORK è demandata al ms timeline-service
        element.setElementId(addTimelineElementResponse.getTimelineElementId());
        return addTimelineElementResponse;
    }

    @Override
    public Long retrieveAndIncrementCounterForTimelineEvent(String timelineId) {
        log.debug("retrieveAndIncrementCounterForTimelineEvent - timelineId={}", timelineId);
        return timelineClient.retrieveAndIncrementCounterForTimelineEvent(timelineId);
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId) {
        log.debug("getTimelineElement - IUN={} and timelineId={}", iun, timelineId);
        return Optional.ofNullable(timelineClient.getTimelineElement(iun, timelineId, false));
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElementStrongly(String iun, String timelineId) {
        log.debug("getTimelineElementStrongly - IUN={} and timelineId={}", iun, timelineId);
        return Optional.ofNullable(timelineClient.getTimelineElement(iun, timelineId, true));
    }

    @Override
    public <T> Optional<T> getTimelineElementDetails(String iun, String timelineId, Class<T> timelineDetailsClass) {
        log.debug("getTimelineElementDetails - IUN={} and timelineId={}", iun, timelineId);

        TimelineElementDetailsInt timelineElementDetailsInt = timelineClient.getTimelineElementDetails(iun, timelineId);

        return castInternalDetails(timelineDetailsClass, timelineElementDetailsInt);
    }

    private <T> @NotNull Optional<T> castInternalDetails(Class<T> timelineDetailsClass, TimelineElementDetailsInt timelineElementDetailsInt) {
        if( timelineElementDetailsInt == null) {
            return Optional.empty();
        }
        return Optional.of(timelineDetailsClass.cast(timelineElementDetailsInt));
    }

    @Override
    public <T> Optional<T> getTimelineElementDetailForSpecificRecipient(String iun, int recIndex, boolean confidentialInfoRequired, TimelineElementCategoryInt category, Class<T> timelineDetailsClass) {
        log.debug("getTimelineElementDetailForSpecificRecipient - IUN={}, recIndex={}, confidentialInfoRequired={}, category={}", iun, recIndex, confidentialInfoRequired, category);

        TimelineElementDetailsInt timelineElementDetailsInt = timelineClient.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, category);
        return castInternalDetails(timelineDetailsClass, timelineElementDetailsInt);
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElementForSpecificRecipient(String iun, int recIndex, TimelineElementCategoryInt category) {
        log.debug("getTimelineElementForSpecificRecipient - IUN={}, recIndex={}, category={}", iun, recIndex, category);
        return Optional.ofNullable(timelineClient.getTimelineElementForSpecificRecipient(iun, recIndex, category));
    }

    @Override
    public Set<TimelineElementInternal> getTimeline(String iun, boolean confidentialInfoRequired) {
        log.debug("getTimeline - IUN={} and confidentialInfoRequired={}", iun, confidentialInfoRequired);

        return new HashSet<>(Optional.ofNullable(timelineClient.getTimeline(iun, confidentialInfoRequired, false, null))
                .orElseGet(Collections::emptyList));
    }

    @Override
    public Set<TimelineElementInternal> getTimelineStrongly(String iun, boolean confidentialInfoRequired) {
        log.debug("getTimelineStrongly - IUN={} and confidentialInfoRequired={}", iun, confidentialInfoRequired);

        return new HashSet<>(Optional.ofNullable(timelineClient.getTimeline(iun, confidentialInfoRequired, true, null))
                .orElseGet(Collections::emptyList));
    }

    @Override
    public Set<TimelineElementInternal> getTimelineByIunTimelineId(String iun, String timelineId, boolean confidentialInfoRequired) {
        log.debug("getTimelineByIunTimelineId - IUN={}, timelineId={}, confidentialInfoRequired={}", iun, timelineId, confidentialInfoRequired);

        return new HashSet<>(Optional.ofNullable(timelineClient.getTimeline(iun, confidentialInfoRequired, false, timelineId))
                .orElseGet(Collections::emptyList));
    }

    @Override
    public NotificationHistoryResponse getTimelineAndStatusHistory(String iun, int recipients, Instant createdAt) {
        log.debug("getTimelineAndStatusHistory - IUN={}, recipients={}, createdAt={}", iun, recipients, createdAt);

        return timelineClient.getTimelineAndStatusHistory(iun, recipients, createdAt);
    }

    public Optional<Instant> getNotificationCancellationRequested(String iun){
        log.debug("getNotificationCancellationRequested - IUN={}", iun);
        try {
            return Optional.ofNullable(timelineClient.getNotificationCancellationRequested(iun));
        } catch (PnHttpResponseException pnHttpResponseException) {
            if (pnHttpResponseException.getStatusCode() == HttpStatus.NOT_FOUND.value()
                    && pnHttpResponseException.getProblem().getErrors().getFirst().getCode().equals(ERROR_CODE_TIMELINESERVICE_TIMELINE_ELEMENT_NOT_PRESENT)) {
                log.debug("Cancellation request not found for iun: {}. Returning empty Optional.", iun);
                return Optional.empty();
            }
            else {
                throw pnHttpResponseException;
            }
        }
    }
}
