package it.pagopa.pn.deliverypushworkflow.action.it.mockbean;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.CheckCoverageRequest;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.CheckCoverageResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.SearchMode;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.raddalt.RaddAltClient;

import java.time.LocalDate;
import java.util.List;

public class RaddAltClientMock implements RaddAltClient {
    private static final List<String> RADD_COVERED_CAP = List.of("80078","80124");

    @Override
    public CheckCoverageResponse checkCoverage(SearchMode searchMode, CheckCoverageRequest checkCoverageRequest, LocalDate searchDate) {
        CheckCoverageResponse checkCoverageResponse= new CheckCoverageResponse();
        checkCoverageResponse.setHasCoverage(RADD_COVERED_CAP.contains(checkCoverageRequest.getCap()));
        return checkCoverageResponse;
    }
}
