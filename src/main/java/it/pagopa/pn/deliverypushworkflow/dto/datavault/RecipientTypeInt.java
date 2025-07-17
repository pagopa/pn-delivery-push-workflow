package it.pagopa.pn.deliverypushworkflow.dto.datavault;

public enum RecipientTypeInt {
    PF("PF"),

    PG("PG");

    private final String value;

    RecipientTypeInt(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
