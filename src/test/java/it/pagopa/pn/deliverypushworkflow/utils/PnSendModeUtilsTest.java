package it.pagopa.pn.deliverypushworkflow.utils;

import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.legalfacts.AarTemplateChooseStrategy;
import it.pagopa.pn.deliverypushworkflow.legalfacts.AarTemplateStrategyFactory;
import it.pagopa.pn.deliverypushworkflow.legalfacts.AarTemplateType;
import it.pagopa.pn.deliverypushworkflow.legalfacts.StaticAarTemplateChooseStrategy;
import it.pagopa.pn.deliverypushworkflow.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypushworkflow.legalfacts.DynamicRADDExperimentationChooseStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.deliverypushworkflow.utils.PnSendModeUtils.*;

@ExtendWith(SpringExtension.class)
class PnSendModeUtilsTest {
    @Mock
    private PnDeliveryPushWorkflowConfigs PnDeliveryPushWorkflowConfigs;
    @Mock
    private AarTemplateStrategyFactory aarTemplateStrategyFactory;
    @Mock
    CheckRADDExperimentation checkRADDExperimentation;

    private PnSendModeUtils pnSendModeUtils;
    
    @Test
    void getPnSendModeNoConfiguration() {
        //GIVEN
        List<String> configurationList = new ArrayList<>();
        String firstCorrectConfiguration = "2022-11-30T23:00:00Z;AAR;AAR;AAR;AAR_NOTIFICATION_RADD";
        String[] arrayObj = firstCorrectConfiguration.split(SEPARATOR);
        Instant correctConfigStartDate = Instant.parse(arrayObj[INDEX_START_DATE]);
        configurationList.add(firstCorrectConfiguration);
        Mockito.when(PnDeliveryPushWorkflowConfigs.getPnSendMode()).thenReturn(configurationList);
        pnSendModeUtils = new PnSendModeUtils(PnDeliveryPushWorkflowConfigs, aarTemplateStrategyFactory);

        Instant time = correctConfigStartDate.minus(1, ChronoUnit.DAYS);
        //WHEN
        PnSendMode pnSendMode = pnSendModeUtils.getPnSendMode(time);
        //THEN
        Assertions.assertNull(pnSendMode);
    }
    
    @Test
    void getPnSendModeOneConfigurationOk() {
        //GIVEN
        List<String> configurationList = new ArrayList<>();
        String firstCorrectConfiguration = "2022-11-30T23:00:00Z;AAR;AAR;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION_RADD";
        String[] arrayObj = firstCorrectConfiguration.split(SEPARATOR);
        Instant correctConfigStartDate = Instant.parse(arrayObj[INDEX_START_DATE]);
        SendAttachmentMode correctAnalogSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[ANALOG_SEND_ATTACHMENT_MODE_INDEX]);
        SendAttachmentMode correctSimpleRegisteredLetterSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[SIMPLE_REGISTERED_LETTER_SEND_ATTACHMENT_MODE_INDEX]);
        AarTemplateType correctAarTemplateType = AarTemplateType.valueOf(arrayObj[AAR_TEMPLATE_TYPE_INDEX]);
        configurationList.add(firstCorrectConfiguration);
        Mockito.when(PnDeliveryPushWorkflowConfigs.getPnSendMode()).thenReturn(configurationList);
        AarTemplateChooseStrategy aarTemplateChooseStrategy = new StaticAarTemplateChooseStrategy(AarTemplateType.valueOf(correctAarTemplateType.name()));
        Mockito.when(aarTemplateStrategyFactory.getAarTemplateStrategy(Mockito.anyString())).thenReturn(aarTemplateChooseStrategy);

        pnSendModeUtils = new PnSendModeUtils(PnDeliveryPushWorkflowConfigs, aarTemplateStrategyFactory);
        Instant time = correctConfigStartDate.plus(1, ChronoUnit.DAYS);

        //WHEN
        PnSendMode pnSendMode = pnSendModeUtils.getPnSendMode(time);
        
        //THEN
        Assertions.assertNotNull(pnSendMode);
        Assertions.assertEquals(correctConfigStartDate, pnSendMode.getStartConfigurationTime());
        Assertions.assertEquals(correctAnalogSendAttachmentMode, pnSendMode.getAnalogSendAttachmentMode());
        Assertions.assertEquals(correctSimpleRegisteredLetterSendAttachmentMode, pnSendMode.getSimpleRegisteredLetterSendAttachmentMode());
        
        AarTemplateType actualAaarTemplateType = pnSendMode.getAarTemplateTypeChooseStrategy().choose(PhysicalAddressInt.builder().build());
        Assertions.assertEquals(correctAarTemplateType, actualAaarTemplateType);
    }

    @Test
    void getPnSendModeOneConfigurationMoreItem() {
        //GIVEN
        List<String> configurationList = new ArrayList<>();
        String firstConfiguration = "2022-11-30T23:00:00Z;AAR;AAR;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION_RADD";
        String secondCorrectConfiguration = "2022-12-20T23:00:00Z;AAR-DOCUMENTS;AAR-DOCUMENTS;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION_RADD";
        String thirdConfiguration = "2022-12-31T23:00:00Z;AAR-DOCUMENTS-PAYMENTS;AAR;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION";
        
        configurationList.add(firstConfiguration);
        configurationList.add(secondCorrectConfiguration);
        configurationList.add(thirdConfiguration);

        String[] arrayObj = secondCorrectConfiguration.split(SEPARATOR);
        Instant correctConfigStartDate = Instant.parse(arrayObj[INDEX_START_DATE]);
        SendAttachmentMode correctAnalogSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[ANALOG_SEND_ATTACHMENT_MODE_INDEX]);
        SendAttachmentMode correctSimpleRegisteredLetterSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[SIMPLE_REGISTERED_LETTER_SEND_ATTACHMENT_MODE_INDEX]);
        AarTemplateType correctAarTemplateType = AarTemplateType.valueOf(arrayObj[AAR_TEMPLATE_TYPE_INDEX]);
        Mockito.when(PnDeliveryPushWorkflowConfigs.getPnSendMode()).thenReturn(configurationList);
        AarTemplateChooseStrategy aarTemplateChooseStrategy = new StaticAarTemplateChooseStrategy(AarTemplateType.valueOf(correctAarTemplateType.name()));
        Mockito.when(aarTemplateStrategyFactory.getAarTemplateStrategy(Mockito.anyString())).thenReturn(aarTemplateChooseStrategy);

        pnSendModeUtils = new PnSendModeUtils(PnDeliveryPushWorkflowConfigs, aarTemplateStrategyFactory);

        
        Instant time = correctConfigStartDate.plus(1, ChronoUnit.DAYS);
        //WHEN
        PnSendMode pnSendMode = pnSendModeUtils.getPnSendMode(time);
        //THEN
        Assertions.assertNotNull(pnSendMode);
        Assertions.assertEquals(correctConfigStartDate, pnSendMode.getStartConfigurationTime());
        Assertions.assertEquals(correctAnalogSendAttachmentMode, pnSendMode.getAnalogSendAttachmentMode());
        Assertions.assertEquals(correctSimpleRegisteredLetterSendAttachmentMode, pnSendMode.getSimpleRegisteredLetterSendAttachmentMode());
        AarTemplateType actualAaarTemplateType = pnSendMode.getAarTemplateTypeChooseStrategy().choose(PhysicalAddressInt.builder().build());
        Assertions.assertEquals(correctAarTemplateType, actualAaarTemplateType);
    }
    
    @Test
    void getPnSendModeOneConfigurationSameDate() {
        //GIVEN
        List<String> configurationList = new ArrayList<>();
        String firstConfiguration = "1970-01-01T00:00:00Z;AAR-DOCUMENTS-PAYMENTS;AAR-DOCUMENTS-PAYMENTS;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION";
        String secondCorrectConfiguration = "2023-11-30T23:00:00Z;AAR;AAR;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION_RADD";

        configurationList.add(firstConfiguration);
        configurationList.add(secondCorrectConfiguration);

        String[] arrayObj = secondCorrectConfiguration.split(SEPARATOR);
        Instant correctConfigStartDate = Instant.parse(arrayObj[INDEX_START_DATE]);
        SendAttachmentMode correctAnalogSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[ANALOG_SEND_ATTACHMENT_MODE_INDEX]);
        SendAttachmentMode correctSimpleRegisteredLetterSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[SIMPLE_REGISTERED_LETTER_SEND_ATTACHMENT_MODE_INDEX]);
        AarTemplateType correctAarTemplateType = AarTemplateType.valueOf(arrayObj[AAR_TEMPLATE_TYPE_INDEX]);

        Mockito.when(PnDeliveryPushWorkflowConfigs.getPnSendMode()).thenReturn(configurationList);
        AarTemplateChooseStrategy aarTemplateChooseStrategy = new StaticAarTemplateChooseStrategy(AarTemplateType.valueOf(correctAarTemplateType.name()));
        Mockito.when(aarTemplateStrategyFactory.getAarTemplateStrategy(Mockito.anyString())).thenReturn(aarTemplateChooseStrategy);

        pnSendModeUtils = new PnSendModeUtils(PnDeliveryPushWorkflowConfigs, aarTemplateStrategyFactory);

        //WHEN
        PnSendMode pnSendMode = pnSendModeUtils.getPnSendMode(correctConfigStartDate);
        
        //THEN
        Assertions.assertNotNull(pnSendMode);
        Assertions.assertEquals(correctConfigStartDate, pnSendMode.getStartConfigurationTime());
        Assertions.assertEquals(correctAnalogSendAttachmentMode, pnSendMode.getAnalogSendAttachmentMode());
        Assertions.assertEquals(correctSimpleRegisteredLetterSendAttachmentMode, pnSendMode.getSimpleRegisteredLetterSendAttachmentMode());
        AarTemplateType actualAaarTemplateType = pnSendMode.getAarTemplateTypeChooseStrategy().choose(PhysicalAddressInt.builder().build());
        Assertions.assertEquals(correctAarTemplateType, actualAaarTemplateType);
    }

    @Test
    void getPnSendModeOneConfigurationSameDateAfter() {
        //GIVEN
        List<String> configurationList = new ArrayList<>();
        String firstConfiguration = "1970-01-01T00:00:00Z;AAR-DOCUMENTS-PAYMENTS;AAR-DOCUMENTS-PAYMENTS;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION";
        String secondCorrectConfiguration = "2023-11-30T23:00:00Z;AAR;AAR;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION_RADD";

        configurationList.add(firstConfiguration);
        configurationList.add(secondCorrectConfiguration);

        String[] arrayObj = secondCorrectConfiguration.split(SEPARATOR);
        Instant correctConfigStartDate = Instant.parse(arrayObj[INDEX_START_DATE]);
        SendAttachmentMode correctAnalogSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[ANALOG_SEND_ATTACHMENT_MODE_INDEX]);
        SendAttachmentMode correctSimpleRegisteredLetterSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[SIMPLE_REGISTERED_LETTER_SEND_ATTACHMENT_MODE_INDEX]);
        AarTemplateType correctAarTemplateType = AarTemplateType.valueOf(arrayObj[AAR_TEMPLATE_TYPE_INDEX]);

        Mockito.when(PnDeliveryPushWorkflowConfigs.getPnSendMode()).thenReturn(configurationList);
        AarTemplateChooseStrategy aarTemplateChooseStrategy = new StaticAarTemplateChooseStrategy(AarTemplateType.valueOf(correctAarTemplateType.name()));
        Mockito.when(aarTemplateStrategyFactory.getAarTemplateStrategy(Mockito.anyString())).thenReturn(aarTemplateChooseStrategy);

        pnSendModeUtils = new PnSendModeUtils(PnDeliveryPushWorkflowConfigs, aarTemplateStrategyFactory);
        
        Instant time = correctConfigStartDate.plus(1, ChronoUnit.DAYS);
        
        //WHEN
        PnSendMode pnSendMode = pnSendModeUtils.getPnSendMode(time);
        
        //THEN
        Assertions.assertNotNull(pnSendMode);
        Assertions.assertEquals(correctConfigStartDate, pnSendMode.getStartConfigurationTime());
        Assertions.assertEquals(correctAnalogSendAttachmentMode, pnSendMode.getAnalogSendAttachmentMode());
        Assertions.assertEquals(correctSimpleRegisteredLetterSendAttachmentMode, pnSendMode.getSimpleRegisteredLetterSendAttachmentMode());
        AarTemplateType actualAaarTemplateType = pnSendMode.getAarTemplateTypeChooseStrategy().choose(PhysicalAddressInt.builder().build());
        Assertions.assertEquals(correctAarTemplateType, actualAaarTemplateType);
    }

    @Test
    void getPnSendModeOneConfigurationSameDateBefore() {
        //GIVEN
        List<String> configurationList = new ArrayList<>();
        String firstCorrectConfiguration = "1970-01-01T00:00:00Z;AAR-DOCUMENTS-PAYMENTS;AAR-DOCUMENTS-PAYMENTS;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION_RADD";
        String secondConfiguration = "2023-11-30T23:00:00Z;AAR;AAR;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION_RADD";

        configurationList.add(firstCorrectConfiguration);
        configurationList.add(secondConfiguration);

        String[] arrayObj = firstCorrectConfiguration.split(SEPARATOR);
        Instant correctConfigStartDate = Instant.parse(arrayObj[INDEX_START_DATE]);
        SendAttachmentMode correctAnalogSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[ANALOG_SEND_ATTACHMENT_MODE_INDEX]);
        SendAttachmentMode correctSimpleRegisteredLetterSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[SIMPLE_REGISTERED_LETTER_SEND_ATTACHMENT_MODE_INDEX]);
        AarTemplateType correctAarTemplateType = AarTemplateType.valueOf(arrayObj[AAR_TEMPLATE_TYPE_INDEX]);

        Mockito.when(PnDeliveryPushWorkflowConfigs.getPnSendMode()).thenReturn(configurationList);
        AarTemplateChooseStrategy aarTemplateChooseStrategy = new StaticAarTemplateChooseStrategy(AarTemplateType.valueOf(correctAarTemplateType.name()));
        Mockito.when(aarTemplateStrategyFactory.getAarTemplateStrategy(Mockito.anyString())).thenReturn(aarTemplateChooseStrategy);

        pnSendModeUtils = new PnSendModeUtils(PnDeliveryPushWorkflowConfigs, aarTemplateStrategyFactory);
    
        String[] secondConfObj = secondConfiguration.split(SEPARATOR);
        Instant secondConfStartDate = Instant.parse(secondConfObj[INDEX_START_DATE]);
        
        Instant time = secondConfStartDate.minus(1, ChronoUnit.SECONDS);
        //WHEN
        PnSendMode pnSendMode = pnSendModeUtils.getPnSendMode(time);
        
        //THEN
        Assertions.assertNotNull(pnSendMode);
        Assertions.assertEquals(correctConfigStartDate, pnSendMode.getStartConfigurationTime());
        Assertions.assertEquals(correctAnalogSendAttachmentMode, pnSendMode.getAnalogSendAttachmentMode());
        Assertions.assertEquals(correctSimpleRegisteredLetterSendAttachmentMode, pnSendMode.getSimpleRegisteredLetterSendAttachmentMode());
        AarTemplateType actualAaarTemplateType = pnSendMode.getAarTemplateTypeChooseStrategy().choose(PhysicalAddressInt.builder().build());
        Assertions.assertEquals(correctAarTemplateType, actualAaarTemplateType);
    }

    @Test
    void getPnSendModeDynamicConfiguration() {
        //GIVEN
        List<String> configurationList = new ArrayList<>();
        String firstConfiguration = "2022-11-30T23:00:00Z;AAR;AAR;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION_RADD";
        String secondCorrectConfiguration = "2022-12-20T23:00:00Z;AAR-DOCUMENTS;AAR-DOCUMENTS;AAR-DOCUMENTS-PAYMENTS;<RADD_TEMPLATE_DEFINITION>";
        String thirdConfiguration = "2022-12-31T23:00:00Z;AAR-DOCUMENTS-PAYMENTS;AAR;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION";

        configurationList.add(firstConfiguration);
        configurationList.add(secondCorrectConfiguration);
        configurationList.add(thirdConfiguration);

        String[] arrayObj = secondCorrectConfiguration.split(SEPARATOR);
        Instant correctConfigStartDate = Instant.parse(arrayObj[INDEX_START_DATE]);
        SendAttachmentMode correctAnalogSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[ANALOG_SEND_ATTACHMENT_MODE_INDEX]);
        SendAttachmentMode correctSimpleRegisteredLetterSendAttachmentMode = SendAttachmentMode.fromValue(arrayObj[SIMPLE_REGISTERED_LETTER_SEND_ATTACHMENT_MODE_INDEX]);
        
        Mockito.when(checkRADDExperimentation.checkAddress(Mockito.any())).thenReturn(true);
        AarTemplateChooseStrategy aarTemplateChooseStrategy = new DynamicRADDExperimentationChooseStrategy(checkRADDExperimentation);
        Mockito.when(aarTemplateStrategyFactory.getAarTemplateStrategy(Mockito.anyString())).thenReturn(aarTemplateChooseStrategy);
        
        AarTemplateChooseStrategy templateChooseStrategy = new DynamicRADDExperimentationChooseStrategy(checkRADDExperimentation);
        Mockito.when(aarTemplateStrategyFactory.getAarTemplateStrategy(Mockito.anyString())).thenReturn(templateChooseStrategy);
        
        Mockito.when(PnDeliveryPushWorkflowConfigs.getPnSendMode()).thenReturn(configurationList);
        pnSendModeUtils = new PnSendModeUtils(PnDeliveryPushWorkflowConfigs, aarTemplateStrategyFactory);

        Instant time = correctConfigStartDate.plus(1, ChronoUnit.DAYS);
        //WHEN
        PnSendMode pnSendMode = pnSendModeUtils.getPnSendMode(time);
        //THEN
        Assertions.assertNotNull(pnSendMode);
        Assertions.assertEquals(correctConfigStartDate, pnSendMode.getStartConfigurationTime());
        Assertions.assertEquals(correctAnalogSendAttachmentMode, pnSendMode.getAnalogSendAttachmentMode());
        Assertions.assertEquals(correctSimpleRegisteredLetterSendAttachmentMode, pnSendMode.getSimpleRegisteredLetterSendAttachmentMode());
        AarTemplateType actualAaarTemplateType = pnSendMode.getAarTemplateTypeChooseStrategy().choose(PhysicalAddressInt.builder().build());
        Assertions.assertEquals(AarTemplateType.AAR_NOTIFICATION_RADD_ALT, actualAaarTemplateType);
    }

    @Test
    void noConfiguration() {
        List<String> configurationList = new ArrayList<>();
        
        Mockito.when(PnDeliveryPushWorkflowConfigs.getPnSendMode()).thenReturn(configurationList);
        pnSendModeUtils = new PnSendModeUtils(PnDeliveryPushWorkflowConfigs, aarTemplateStrategyFactory);

        Instant time = Instant.now();
        PnSendMode pnSendMode = pnSendModeUtils.getPnSendMode(time);

        Assertions.assertNull(pnSendMode);
    }

}