package it.pagopa.pn.deliverypushworkflow.utils;

import it.pagopa.pn.deliverypushworkflow.legalfacts.AarTemplateChooseStrategy;
import it.pagopa.pn.deliverypushworkflow.legalfacts.AarTemplateStrategyFactory;
import it.pagopa.pn.deliverypushworkflow.legalfacts.DynamicRADDExperimentationChooseStrategy;
import it.pagopa.pn.deliverypushworkflow.legalfacts.StaticAarTemplateChooseStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static it.pagopa.pn.deliverypushworkflow.action.utils.AarUtils.*;


class AarTemplateStrategyFactoryTest {
    @Mock
    private CheckRADDExperimentation checkRADDExperimentation;
    
    private AarTemplateStrategyFactory aarTemplateStrategyFactory;
    
    @BeforeEach
    public void init(){
        aarTemplateStrategyFactory = new AarTemplateStrategyFactory(checkRADDExperimentation);
    }
    
    @Test
    void getAarTemplateStrategyDynamic() {
        //GIVEN
        String baseAarTemplateType = START_DYNAMIC_PROPERTY_CHARACTER + RADD_DYNAMIC_TEMPLATE_VALUE + END_DYNAMIC_PROPERTY_CHARACTER;

        //WHEN
        AarTemplateChooseStrategy aarTemplateChooseStrategy = aarTemplateStrategyFactory.getAarTemplateStrategy(baseAarTemplateType);
        
        //THEN
        Assertions.assertTrue(aarTemplateChooseStrategy instanceof DynamicRADDExperimentationChooseStrategy);
    }

    @Test
    void getAarTemplateStrategyBasic() {
        //GIVEN
        String baseAarTemplateType = "AAR_NOTIFICATION_RADD";

        //WHEN
        AarTemplateChooseStrategy aarTemplateChooseStrategy = aarTemplateStrategyFactory.getAarTemplateStrategy(baseAarTemplateType);

        //THEN
        Assertions.assertTrue(aarTemplateChooseStrategy instanceof StaticAarTemplateChooseStrategy);
    }
}