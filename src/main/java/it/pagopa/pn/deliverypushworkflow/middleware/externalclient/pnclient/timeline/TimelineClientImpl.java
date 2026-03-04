package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.timeline;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;

import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.AddTimelineElementResponse;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.api.TimelineControllerApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.*;
import it.pagopa.pn.deliverypushworkflow.service.mapper.TimelineServiceMapper;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_TIMELINESERVICE_TIMELINE_ELEMENT_NOT_PRESENT;

@CustomLog
@RequiredArgsConstructor
@Component
public class TimelineClientImpl implements TimelineClient {
    private final TimelineControllerApi timelineControllerApi;
    private final TimelineServiceMapper timelineServiceMapper;

    @Override
    public AddTimelineElementResponse addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        log.logInvokingExternalService(CLIENT_NAME, ADD_TIMELINE_ELEMENT);
        NewTimelineElement newTimelineElement = timelineServiceMapper.getNewTimelineElement(element, notification);
        try {
            TimelineElementIdResponse timelineElementIdResponse = timelineControllerApi.addTimelineElement(newTimelineElement);
            return new AddTimelineElementResponse(timelineElementIdResponse.getElementId(), false);
        } catch (PnHttpResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.SC_CONFLICT) {
                log.warn("Exception idconflict is expected for retry, letting flow continue");
                return new AddTimelineElementResponse(null, true);
            }

            log.error("Error while invoking {}: {}", ADD_TIMELINE_ELEMENT, ex.getMessage(), ex);
            throw ex;
        }

    }

    @Override
    public Long retrieveAndIncrementCounterForTimelineEvent(String timelineId) {
        log.logInvokingExternalService(CLIENT_NAME, RETRIEVE_AND_INCREMENT_COUNTER_FOR_TIMELINE_EVENT);

        return timelineControllerApi.retrieveAndIncrementCounterForTimelineEvent(timelineId);
    }

    @Override
    public TimelineElementInternal getTimelineElement(String iun, String timelineId, Boolean strongly) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_ELEMENT);

        TimelineElement timelineElement = timelineControllerApi.getTimelineElement(iun, timelineId, strongly);
        return timelineServiceMapper.toTimelineElementInternal(timelineElement);
    }

    @Override
    public TimelineElementDetailsInt getTimelineElementDetails(String iun, String timelineId) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_ELEMENT_DETAILS);

        TimelineElementDetails timelineElementDetails = timelineControllerApi.getTimelineElementDetails(iun, timelineId);
        if(timelineElementDetails == null) {
            return null;
        }
        return timelineServiceMapper.toTimelineElementDetailsInt(timelineElementDetails, TimelineElementCategoryInt.valueOf(timelineElementDetails.getCategoryType()));
    }

    @Override
    public TimelineElementDetailsInt getTimelineElementDetailForSpecificRecipient(String iun, Integer recIndex, Boolean confidentialInfoRequired, TimelineElementCategoryInt category) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_ELEMENT_DETAIL_FOR_SPECIFIC_RECIPIENT);

        TimelineElementDetails timelineElementDetails = timelineControllerApi.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, TimelineCategory.fromValue(category.name()));
        return timelineServiceMapper.toTimelineElementDetailsInt(timelineElementDetails, category);
    }

    @Override
    public TimelineElementInternal getTimelineElementForSpecificRecipient(String iun, Integer recIndex, TimelineElementCategoryInt category) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_ELEMENT_FOR_SPECIFIC_RECIPIENT);

        TimelineElement timelineElement = timelineControllerApi.getTimelineElementForSpecificRecipient(iun, recIndex, TimelineCategory.fromValue(category.name()));
        return timelineServiceMapper.toTimelineElementInternal(timelineElement);
    }

    @Override
    public List<TimelineElementInternal> getTimeline(String iun, Boolean confidentialInfoRequired, Boolean strongly, String timelineId) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE);

        return timelineControllerApi.getTimeline(iun, confidentialInfoRequired, strongly, timelineId)
                .stream()
                .filter(element -> TimelineElementCategoryInt.isKnownCategory(element.getCategory().getValue()))
                .map(timelineServiceMapper::toTimelineElementInternal)
                .toList();
    }

    @Override
    public NotificationHistoryResponse getTimelineAndStatusHistory(String iun, int recipients, Instant createdAt) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_AND_STATUS_HISTORY);

        return timelineControllerApi.getTimelineAndStatusHistory(iun, recipients, createdAt);
    }

    @Override
    public Optional<CancellationRequestResponse> getNotificationCancellationRequested(String iun) {
        log.logInvokingExternalService(CLIENT_NAME, GET_NOTIFICATION_CANCELLATION_REQUESTED);
        try {
            return Optional.ofNullable(timelineControllerApi.getCancellationRequest(iun));
        } catch (PnHttpResponseException pnHttpResponseException) {
            if (pnHttpResponseException.getStatusCode() == org.springframework.http.HttpStatus.NOT_FOUND.value()
                    && pnHttpResponseException.getProblem().getErrors().getFirst().getCode().equals(ERROR_CODE_TIMELINESERVICE_TIMELINE_ELEMENT_NOT_PRESENT)) {
                log.debug("Cancellation request not found for iun: {}. Returning empty optional.", iun);
                return Optional.empty();
            }
            else {
                throw pnHttpResponseException;
            }
        }
    }
}
