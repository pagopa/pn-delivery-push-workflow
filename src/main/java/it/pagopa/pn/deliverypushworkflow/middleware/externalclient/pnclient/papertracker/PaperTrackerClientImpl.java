package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.papertracker;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.api.PaperTrackerTrackingApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.TrackingsRequest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.TrackingsResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@CustomLog
@RequiredArgsConstructor
public class PaperTrackerClientImpl implements PaperTrackerClient {
    private final PaperTrackerTrackingApi paperTracking;

    public TrackingsResponse getTrackingResponse(String prepareRequestId) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TRACKING_RESPONSE);
        TrackingsRequest request = new TrackingsRequest();
        request.setTrackingIds(List.of(prepareRequestId));
        return paperTracking.retrieveTrackings(request);
    }
}
