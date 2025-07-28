package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NewTimelineElement;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.TimelineCategory;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.TimelineElement;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.TimelineElementDetails;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.timeline.TimelineClient;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import it.pagopa.pn.deliverypushworkflow.service.mapper.TimelineServiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TimelineServiceHttpImpl implements TimelineService {

    private final TimelineClient timelineClient;

    @Override
    public boolean addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        log.info("addTimelineElement - IUN={} and timelineId={}", element.getIun(), element.getElementId());

        NewTimelineElement newTimelineElement = TimelineServiceMapper.getNewTimelineElement(element, notification);
        return timelineClient.addTimelineElement(newTimelineElement);
    }

    @Override
    public Long retrieveAndIncrementCounterForTimelineEvent(String timelineId) {
        log.debug("retrieveAndIncrementCounterForTimelineEvent - timelineId={}", timelineId);

        return timelineClient.retrieveAndIncrementCounterForTimelineEvent(timelineId);
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId) {
        log.debug("getTimelineElement - IUN={} and timelineId={}", iun, timelineId);

        TimelineElement timelineElement = timelineClient.getTimelineElement(iun, timelineId, false);
        return Optional.ofNullable(TimelineServiceMapper.toTimelineElementInternal(timelineElement));
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElementStrongly(String iun, String timelineId) {
        log.debug("getTimelineElementStrongly - IUN={} and timelineId={}", iun, timelineId);

        TimelineElement timelineElement = timelineClient.getTimelineElement(iun, timelineId, true);
        return Optional.ofNullable(TimelineServiceMapper.toTimelineElementInternal(timelineElement));
    }

    @Override
    public <T> Optional<T> getTimelineElementDetails(String iun, String timelineId, Class<T> timelineDetailsClass) {
        log.debug("getTimelineElementDetails - IUN={} and timelineId={}", iun, timelineId);

        TimelineElementDetails timelineElementDetails = timelineClient.getTimelineElementDetails(iun, timelineId);

        return getTimelineElementDetailsInt(timelineDetailsClass, timelineElementDetails);
    }

    private static <T> @NotNull Optional<T> getTimelineElementDetailsInt(Class<T> timelineDetailsClass, TimelineElementDetails timelineElementDetails) {
        if( timelineElementDetails == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(timelineDetailsClass.cast(TimelineServiceMapper.toTimelineElementDetailsInt(
                timelineElementDetails, TimelineElementCategoryInt.valueOf(timelineElementDetails.getCategoryType()))));
    }

    @Override
    public <T> Optional<T> getTimelineElementDetailForSpecificRecipient(String iun, int recIndex, boolean confidentialInfoRequired, TimelineElementCategoryInt category, Class<T> timelineDetailsClass) {
        log.debug("getTimelineElementDetailForSpecificRecipient - IUN={}, recIndex={}, confidentialInfoRequired={}, category={}", iun, recIndex, confidentialInfoRequired, category);

        TimelineElementDetails timelineElementDetails = timelineClient.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, TimelineCategory.fromValue(category.name()));
        return getTimelineElementDetailsInt(timelineDetailsClass,timelineElementDetails);
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElementForSpecificRecipient(String iun, int recIndex, TimelineElementCategoryInt category) {
        log.debug("getTimelineElementForSpecificRecipient - IUN={}, recIndex={}, category={}", iun, recIndex, category);

        TimelineElement timelineElement = timelineClient.getTimelineElementForSpecificRecipient(iun, recIndex, TimelineCategory.fromValue(category.name()));
        return Optional.ofNullable(TimelineServiceMapper.toTimelineElementInternal(timelineElement));
    }

    @Override
    public Set<TimelineElementInternal> getTimeline(String iun, boolean confidentialInfoRequired) {
        log.debug("getTimeline - IUN={} and confidentialInfoRequired={}", iun, confidentialInfoRequired);

        return Optional.ofNullable(timelineClient.getTimeline(iun, confidentialInfoRequired, false, null))
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(timelineElement -> TimelineElementCategoryInt.isKnownCategory(timelineElement.getCategory().getValue()))
                .map(TimelineServiceMapper::toTimelineElementInternal)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<TimelineElementInternal> getTimelineStrongly(String iun, boolean confidentialInfoRequired) {
        log.debug("getTimelineStrongly - IUN={} and confidentialInfoRequired={}", iun, confidentialInfoRequired);

        return Optional.ofNullable(timelineClient.getTimeline(iun, confidentialInfoRequired, true, null))
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(timelineElement -> TimelineElementCategoryInt.isKnownCategory(timelineElement.getCategory().getValue()))
                .map(TimelineServiceMapper::toTimelineElementInternal)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<TimelineElementInternal> getTimelineByIunTimelineId(String iun, String timelineId, boolean confidentialInfoRequired) {
        log.debug("getTimelineByIunTimelineId - IUN={}, timelineId={}, confidentialInfoRequired={}", iun, timelineId, confidentialInfoRequired);

        return Optional.ofNullable(timelineClient.getTimeline(iun, confidentialInfoRequired, false, timelineId))
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(timelineElement -> TimelineElementCategoryInt.isKnownCategory(timelineElement.getCategory().getValue()))
                .map(TimelineServiceMapper::toTimelineElementInternal)
                .collect(Collectors.toSet());
    }
}
