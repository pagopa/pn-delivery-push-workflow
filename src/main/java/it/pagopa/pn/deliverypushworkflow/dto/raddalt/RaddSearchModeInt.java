package it.pagopa.pn.deliverypushworkflow.dto.raddalt;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.SearchMode;

public enum RaddSearchModeInt {
    OLD(null),
    LIGHT(SearchMode.LIGHT),
    COMPLETE(SearchMode.COMPLETE);

    private final SearchMode clientSearchMode;

    RaddSearchModeInt(SearchMode clientSearchMode) {
        this.clientSearchMode = clientSearchMode;
    }

    public SearchMode toClientSearchMode() {
        if(this.clientSearchMode == null) {
            throw new IllegalStateException(
                    "RaddSearchMode." + this.name() + " non è compatibile con il client HTTP"
            );
        }

        return this.clientSearchMode;
    }
}
