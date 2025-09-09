package it.pagopa.pn.deliverypushworkflow.action.it.mockbean;

import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.EventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.BaseAnalogDetailsInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.PaperStatus;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.Tracking;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.TrackingsResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.papertracker.PaperTrackerClient;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

@Slf4j
public class PaperTrackerClientMock implements PaperTrackerClient {
    private final TimelineClientMock timelineClientMock;
    private final TimelineUtils timelineUtils;

    public PaperTrackerClientMock(TimelineClientMock timelineClientMock,
                                  TimelineUtils timelineUtils){
        this.timelineClientMock = timelineClientMock;
        this.timelineUtils = timelineUtils;
    }

    // Restituisce un oggetto TrackingsResponse che viene valutato come presenza di DEMAT solo quando è presente in timeline
    // un elemento con categoria SEND_ANALOG_FEEDBACK per lo stesso destinatario e numero di tentativo di consegna legato al
    // prepareRequestId.
    public TrackingsResponse getTrackingResponseAndTimelineFeedback(String prepareRequestId) {
        String iun = timelineUtils.getIunFromTimelineId(prepareRequestId);
        TimelineElementInternal timelineElement = timelineClientMock.getTimelineElement(iun, prepareRequestId, false);
        if (timelineElement != null && timelineElement.getDetails() instanceof BaseAnalogDetailsInt baseAnalogDetails) {
            TimelineElementInternal analogFeedbackElement = searchSendAnalogFeedback(baseAnalogDetails, iun);
            TrackingsResponse response = new TrackingsResponse();
            if (analogFeedbackElement != null) {
                log.debug("Found send analog feedback for prepareRequestId: {}", prepareRequestId);
                response.setTrackings(List.of(getTracking()));
            }
            return response;
        }
        log.debug("No timeline element found for prepareRequestId: {}", prepareRequestId);
        return null;
    }

    private TimelineElementInternal searchSendAnalogFeedback(BaseAnalogDetailsInt baseAnalogDetails, String iun) {
        String sendAnalogFeedBackEventId = TimelineEventId.SEND_ANALOG_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(baseAnalogDetails.getRecIndex())
                        .sentAttemptMade(baseAnalogDetails.getSentAttemptMade())
                        .build());

        return timelineClientMock.getTimelineElement(iun, sendAnalogFeedBackEventId, false);
    }

    @Override
    public TrackingsResponse getTrackingResponse(String prepareRequestId) {
        return getTrackingResponseAndTimelineFeedback(prepareRequestId);
    }

    private Tracking getTracking() {
        Tracking tracking = new Tracking();
        tracking.setTrackingId("trackingId");
        tracking.setPcRetry("3");
        tracking.setPaperStatus(new PaperStatus());
        Objects.requireNonNull(tracking.getPaperStatus()).setFinalDematFound(true);
        return tracking;
    }
}
