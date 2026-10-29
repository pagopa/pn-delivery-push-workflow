package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReceiverAddressSourceTypeInt {

    MITTENTE("MITTENTE"),
    ANPR("ANPR"),
    REGISTRO_IMPRESE("REGISTRO_IMPRESE"),
    INDAGINE("INDAGINE");

    private final String value;

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
