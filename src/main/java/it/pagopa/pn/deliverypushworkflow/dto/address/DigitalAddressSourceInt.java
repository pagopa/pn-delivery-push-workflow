package it.pagopa.pn.deliverypushworkflow.dto.address;

import lombok.Getter;

@Getter
public enum DigitalAddressSourceInt {
    PLATFORM("PLATFORM"),

    SPECIAL("SPECIAL"),

    GENERAL("GENERAL");

    private final String value;

    DigitalAddressSourceInt(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
