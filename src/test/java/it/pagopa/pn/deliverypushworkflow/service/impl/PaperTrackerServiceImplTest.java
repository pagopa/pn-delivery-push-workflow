package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.Tracking;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.TrackingsResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.ValidationFlow;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.papertracker.PaperTrackerClient;
import it.pagopa.pn.deliverypushworkflow.service.PaperTrackerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
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
        Tracking tracking = new Tracking();
        tracking.setPcRetry("0");
        ValidationFlow validationFlow = new ValidationFlow();
        validationFlow.setDematValidationTimestamp(Instant.now());
        tracking.setValidationFlow(validationFlow);
        trackingsResponse.setTrackings(List.of(tracking));
        Mockito.when(paperTrackerClient.getTrackingResponse(prepareAnalogDomicileTimelineId))
                .thenReturn(trackingsResponse);

        boolean result = paperTrackerService.isPresentDematForPrepareRequest(prepareAnalogDomicileTimelineId);

        Mockito.verify(paperTrackerClient).getTrackingResponse(prepareAnalogDomicileTimelineId);
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
        Mockito.when(paperTrackerClient.getTrackingResponse(prepareAnalogDomicileTimelineId))
                .thenReturn(trackingsResponse);

        boolean result = paperTrackerService.isPresentDematForPrepareRequest(prepareAnalogDomicileTimelineId);

        Mockito.verify(paperTrackerClient).getTrackingResponse(prepareAnalogDomicileTimelineId);
        Assertions.assertFalse(result);
    }

    @Test
    void isPresentDematForPrepareRequestIsFalseForMissingDemat2() {
        String prepareAnalogDomicileTimelineId = "prepareAnalogDomicileTimelineId";

        TrackingsResponse trackingsResponse = new TrackingsResponse();
        Tracking tracking = new Tracking();
        tracking.setPcRetry("0");
        trackingsResponse.setTrackings(List.of(tracking));
        Mockito.when(paperTrackerClient.getTrackingResponse(prepareAnalogDomicileTimelineId))
                .thenReturn(trackingsResponse);

        boolean result = paperTrackerService.isPresentDematForPrepareRequest(prepareAnalogDomicileTimelineId);

        Mockito.verify(paperTrackerClient).getTrackingResponse(prepareAnalogDomicileTimelineId);
        Assertions.assertFalse(result);
    }

    @Test
    void isPresentDematForPrepareRequestIsFalseForMissingDemat3() {
        String prepareAnalogDomicileTimelineId = "prepareAnalogDomicileTimelineId";

        Mockito.when(paperTrackerClient.getTrackingResponse(prepareAnalogDomicileTimelineId))
                .thenReturn(new TrackingsResponse());

        boolean result = paperTrackerService.isPresentDematForPrepareRequest(prepareAnalogDomicileTimelineId);

        Mockito.verify(paperTrackerClient).getTrackingResponse(prepareAnalogDomicileTimelineId);
        Assertions.assertFalse(result);
    }

}
