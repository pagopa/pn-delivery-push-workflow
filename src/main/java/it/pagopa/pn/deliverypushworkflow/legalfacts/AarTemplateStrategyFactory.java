package it.pagopa.pn.deliverypushworkflow.legalfacts;

import it.pagopa.pn.deliverypushworkflow.dto.utils.AarUtils;

import it.pagopa.pn.deliverypushworkflow.util.CheckRADDExperimentation;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AarTemplateStrategyFactory {
    private final CheckRADDExperimentation checkRADDExperimentation;

    public AarTemplateChooseStrategy getAarTemplateStrategy(String baseAarTemplateType){
        if(AarUtils.needDynamicAarRADDDefinition(baseAarTemplateType)){
            return new DynamicRADDExperimentationChooseStrategy(checkRADDExperimentation);
        }else{
            final AarTemplateType aarTemplateType = AarTemplateType.valueOf(baseAarTemplateType);
            return new StaticAarTemplateChooseStrategy(aarTemplateType);
        }
    }
}
