package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.papertracker;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.api.PaperTrackerTrackingApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.TrackingsResponse;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@CustomLog
@RequiredArgsConstructor
public class PaperTrackerClientImpl implements PaperTrackerClient {
    private final PaperTrackerTrackingApi paperTracking;

    public TrackingsResponse retrieveTrackingsByAttemptId(String prepareRequestId) {
        log.logInvokingExternalService(CLIENT_NAME, RETRIEVE_TRACKINGS_BY_ATTEMPTID);
        return paperTracking.retrieveTrackingsByAttemptId(prepareRequestId, null);
    }
}
