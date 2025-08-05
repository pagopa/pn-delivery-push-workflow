package it.pagopa.pn.deliverypushworkflow.action.it.mockbean;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.Tracking;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.TrackingsResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.ValidationFlow;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.papertracker.PaperTrackerClient;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class PaperTrackerClientMock implements PaperTrackerClient {

    @Override
    public TrackingsResponse getTrackingResponse(String prepareRequestId) {
        TrackingsResponse trackingsResponse = new TrackingsResponse();
        trackingsResponse.setTrackings(List.of(getTracking()));
        trackingsResponse.getTrackings().getFirst().setValidationFlow(new ValidationFlow());
        Objects.requireNonNull(trackingsResponse.getTrackings().getFirst().getValidationFlow()).setDematValidationTimestamp(Instant.now());
        return new TrackingsResponse();
    }

    private Tracking getTracking() {
        Tracking tracking = new Tracking();
        tracking.setTrackingId("trackingId");
        tracking.setPcRetry("3");
        tracking.setValidationFlow(new ValidationFlow());
        Objects.requireNonNull(tracking.getValidationFlow()).setDematValidationTimestamp(Instant.now());
        return tracking;
    }
}
