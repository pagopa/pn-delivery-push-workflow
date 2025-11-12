package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.raddalt;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.CheckCoverageRequest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.CheckCoverageResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.SearchMode;

import java.time.LocalDate;

public interface RaddAltClient {
    String CLIENT_NAME = "pn-radd-alt"; //Todo: sostituire con riferimento alla commons
    String CHECK_COVERAGE_PROCESS_NAME = "CHECK COVERAGE";
    CheckCoverageResponse checkCoverage(SearchMode searchMode, CheckCoverageRequest checkCoverageRequest, LocalDate searchDate);
}
