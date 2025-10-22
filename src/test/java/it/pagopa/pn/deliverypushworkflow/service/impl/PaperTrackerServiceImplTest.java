package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.PaperStatus;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.Tracking;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.TrackingsResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.ValidationFlow;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.papertracker.PaperTrackerClient;
import it.pagopa.pn.deliverypushworkflow.service.PaperTrackerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

class PaperTrackerServiceImplTest {
    private PaperTrackerClient paperTrackerClient;
    private PaperTrackerService paperTrackerService;

    @BeforeEach
    public void setUp() {
        paperTrackerClient = Mockito.mock(PaperTrackerClient.class);
        paperTrackerService = new PaperTrackerServiceImpl(paperTrackerClient);
    }

    @Test
    void isPresentDematForPrepareRequestIsTrue() {
        String prepareAnalogDomicileTimelineId = "prepareAnalogDomicileTimelineId";

        TrackingsResponse trackingsResponse = new TrackingsResponse();
        Tracking tracking1 = new Tracking();
        tracking1.setPcRetry("PCRETRY_1");
        PaperStatus paperStatus1 = new PaperStatus();
        paperStatus1.setFinalDematFound(false);
        tracking1.setPaperStatus(paperStatus1);
        Tracking tracking2 = new Tracking();
        tracking2.setPcRetry("PCRETRY_10");
        PaperStatus paperStatus2 = new PaperStatus();
        paperStatus2.setFinalDematFound(true);
        tracking2.setPaperStatus(paperStatus2);
        trackingsResponse.setTrackings(List.of(tracking1, tracking2));
        Mockito.when(paperTrackerClient.retrieveTrackingsByAttemptId(prepareAnalogDomicileTimelineId))
                .thenReturn(trackingsResponse);

        boolean result = paperTrackerService.isPresentDematForPrepareRequest(prepareAnalogDomicileTimelineId);

        Mockito.verify(paperTrackerClient).retrieveTrackingsByAttemptId(prepareAnalogDomicileTimelineId);
        Assertions.assertTrue(result);
    }

    @Test
    void isPresentDematForPrepareRequestIsFalseForMissingDemat() {
        String prepareAnalogDomicileTimelineId = "prepareAnalogDomicileTimelineId";

        TrackingsResponse trackingsResponse = new TrackingsResponse();
        Tracking tracking = new Tracking();
        tracking.setPcRetry("0");
        tracking.setValidationFlow(new ValidationFlow());
        trackingsResponse.setTrackings(List.of(tracking));
        Mockito.when(paperTrackerClient.retrieveTrackingsByAttemptId(prepareAnalogDomicileTimelineId))
                .thenReturn(trackingsResponse);

        boolean result = paperTrackerService.isPresentDematForPrepareRequest(prepareAnalogDomicileTimelineId);

        Mockito.verify(paperTrackerClient).retrieveTrackingsByAttemptId(prepareAnalogDomicileTimelineId);
        Assertions.assertFalse(result);
    }

    @Test
    void isPresentDematForPrepareRequestIsFalseForMissingDemat2() {
        String prepareAnalogDomicileTimelineId = "prepareAnalogDomicileTimelineId";

        TrackingsResponse trackingsResponse = new TrackingsResponse();
        Tracking tracking = new Tracking();
        tracking.setPcRetry("0");
        trackingsResponse.setTrackings(List.of(tracking));
        Mockito.when(paperTrackerClient.retrieveTrackingsByAttemptId(prepareAnalogDomicileTimelineId))
                .thenReturn(trackingsResponse);

        boolean result = paperTrackerService.isPresentDematForPrepareRequest(prepareAnalogDomicileTimelineId);

        Mockito.verify(paperTrackerClient).retrieveTrackingsByAttemptId(prepareAnalogDomicileTimelineId);
        Assertions.assertFalse(result);
    }

    @Test
    void isPresentDematForPrepareRequestIsFalseForMissingDemat3() {
        String prepareAnalogDomicileTimelineId = "prepareAnalogDomicileTimelineId";

        Mockito.when(paperTrackerClient.retrieveTrackingsByAttemptId(prepareAnalogDomicileTimelineId))
                .thenReturn(new TrackingsResponse());

        boolean result = paperTrackerService.isPresentDematForPrepareRequest(prepareAnalogDomicileTimelineId);

        Mockito.verify(paperTrackerClient).retrieveTrackingsByAttemptId(prepareAnalogDomicileTimelineId);
        Assertions.assertFalse(result);
    }

}
