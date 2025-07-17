package it.pagopa.pn.deliverypushworkflow.legalfacts;


import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;

public interface AarTemplateChooseStrategy {
    AarTemplateType choose(PhysicalAddressInt address);
}
