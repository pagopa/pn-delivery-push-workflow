package it.pagopa.pn.deliverypushworkflow.dto.rework;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode
public class ReworkError {
    private String cause;
    private String description;
}
