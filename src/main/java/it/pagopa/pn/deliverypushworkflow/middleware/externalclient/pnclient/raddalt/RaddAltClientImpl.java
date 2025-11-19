package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.raddalt;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.api.CoveragePrivateApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.CheckCoverageRequest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.CheckCoverageResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.SearchMode;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@RequiredArgsConstructor
@CustomLog
@Component
public class RaddAltClientImpl extends CommonBaseClient implements RaddAltClient {
    private final CoveragePrivateApi coveragePrivateApi;

    @Override
    public CheckCoverageResponse checkCoverage(SearchMode searchMode, CheckCoverageRequest checkCoverageRequest, LocalDate searchDate) {
        log.logInvokingExternalDownstreamService(CLIENT_NAME, CHECK_COVERAGE_PROCESS_NAME);
        return coveragePrivateApi.checkCoverage(searchMode, checkCoverageRequest, searchDate);
    }
}
