package it.pagopa.pn.deliverypushworkflow.dto.documentcreation;

import lombok.*;


@AllArgsConstructor
@Getter
@ToString
public enum DocumentCategoryInt {
    AAR("AAR");
    private final String value;
    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
