package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.Tracking;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.papertracker.model.TrackingsResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.papertracker.PaperTrackerClient;
import it.pagopa.pn.deliverypushworkflow.service.PaperTrackerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperTrackerServiceImpl implements PaperTrackerService {

    private final PaperTrackerClient paperTrackerClient;

    @Override
    public boolean isPresentDematForPrepareRequest(String prepareRequestId) {
        log.info("Invoking PaperTrackerClient to check trackingsRequest: {}", prepareRequestId);
        TrackingsResponse response = paperTrackerClient.retrieveTrackingsByAttemptId(prepareRequestId);
        List<Tracking> trackings = response.getTrackings();

        Optional<Tracking> maxPcRetryOpt = trackings.stream()
                .filter(t -> t.getPcRetry() != null && !t.getPcRetry().isEmpty())
                .max(Comparator.comparingInt(t -> Integer.parseInt(removePrefixPcRetry(t.getPcRetry()))));
        if (maxPcRetryOpt.isPresent()) {
            log.info("Found tracking with max pcRetry: {}", maxPcRetryOpt.get().getPcRetry());
            Tracking maxPcRetryTracking = maxPcRetryOpt.get();
            return maxPcRetryTracking.getPaperStatus() != null
                    && Boolean.TRUE.equals(maxPcRetryTracking.getPaperStatus().getFinalDematFound());
        } else {
            log.info("No tracking found with pcRetry, returning false");
            return false;
        }
    }

    private String removePrefixPcRetry(String pcRetry) {
        return pcRetry.replaceFirst("^PCRETRY_", "");
    }
}
