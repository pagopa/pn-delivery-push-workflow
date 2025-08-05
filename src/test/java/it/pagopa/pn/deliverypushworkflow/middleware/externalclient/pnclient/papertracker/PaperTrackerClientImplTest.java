package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.papertracker;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.api.PaperTrackerTrackingApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.TrackingsResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PaperTrackerClientImplTest {

    private PaperTrackerTrackingApi paperTracking;

    private PaperTrackerClientImpl paperTrackerClient;

    @BeforeEach
    void setup() {
        paperTracking = Mockito.mock(PaperTrackerTrackingApi.class);
        paperTrackerClient = new PaperTrackerClientImpl(paperTracking);
    }

    @Test
    void getTrackingResponse() {
        Mockito.when(paperTracking.retrieveTrackings(Mockito.any()))
                .thenReturn(new TrackingsResponse());

        TrackingsResponse response = paperTrackerClient.getTrackingResponse("trackingsRequest");

        Assertions.assertNotNull(response);
    }

}
