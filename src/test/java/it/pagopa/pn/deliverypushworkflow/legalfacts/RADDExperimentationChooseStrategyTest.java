package it.pagopa.pn.deliverypushworkflow.legalfacts;


import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.utils.CheckRADDExperimentation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RADDExperimentationChooseStrategyTest {
    @Mock
    private CheckRADDExperimentation checkRADDExperimentation;
    private DynamicRADDExperimentationChooseStrategy raddExperimentationChooseStrategy;
    
    @BeforeEach
    public void init(){
        raddExperimentationChooseStrategy = new DynamicRADDExperimentationChooseStrategy(checkRADDExperimentation);
    }
    @Test
    void chooseRAADalt() {
        //GIVEN
        Mockito.when(checkRADDExperimentation.checkAddress(Mockito.any(PhysicalAddressInt.class),Mockito.any(NotificationInt.class)))
                .thenReturn(true);
        PhysicalAddressInt address = PhysicalAddressInt.builder()
                .build();
        NotificationInt notificationInt = NotificationInt.builder().build();
        
        //WHEN
        AarTemplateType aarTemplateType = raddExperimentationChooseStrategy.choose(address,notificationInt);
        //THEN
        Assertions.assertEquals(AarTemplateType.AAR_NOTIFICATION_RADD_ALT, aarTemplateType);
    }

    @Test
    void chooseDefault() {
        //GIVEN
        Mockito.when(checkRADDExperimentation.checkAddress(Mockito.any(PhysicalAddressInt.class),Mockito.any(NotificationInt.class)))
                .thenReturn(false);
        PhysicalAddressInt address = PhysicalAddressInt.builder()
                .build();
        NotificationInt notificationInt = NotificationInt.builder().build();

        //WHEN
        AarTemplateType aarTemplateType = raddExperimentationChooseStrategy.choose(address,notificationInt);
        //THEN
        Assertions.assertEquals(AarTemplateType.AAR_NOTIFICATION, aarTemplateType);
    }
}