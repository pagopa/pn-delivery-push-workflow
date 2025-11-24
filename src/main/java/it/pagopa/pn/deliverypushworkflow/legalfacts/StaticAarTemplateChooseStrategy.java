package it.pagopa.pn.deliverypushworkflow.legalfacts;


import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class StaticAarTemplateChooseStrategy implements AarTemplateChooseStrategy{
    private final AarTemplateType aarTemplateType;
    
    @Override
    public AarTemplateType choose(PhysicalAddressInt address, NotificationInt notificationInt) {
        log.debug("Choosing Static AAR type for zip={}", address.getZip());
        return aarTemplateType;
    }
}
