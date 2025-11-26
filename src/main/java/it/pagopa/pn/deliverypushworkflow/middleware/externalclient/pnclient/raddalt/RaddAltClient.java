package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.raddalt;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.CheckCoverageRequest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.CheckCoverageResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.SearchMode;

import java.time.LocalDate;

public interface RaddAltClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_PAPER_CHANNEL;
    String CHECK_COVERAGE_PROCESS_NAME = "CHECK COVERAGE";
    CheckCoverageResponse checkCoverage(SearchMode searchMode, CheckCoverageRequest checkCoverageRequest, LocalDate searchDate);
}
