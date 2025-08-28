package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.papertracker;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.TrackingsResponse;

public interface PaperTrackerClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_PAPER_TRACKER;
    String GET_TRACKING_RESPONSE = "GET TRACKING RESPONSE";

    TrackingsResponse getTrackingResponse(String prepareRequestId);
}
