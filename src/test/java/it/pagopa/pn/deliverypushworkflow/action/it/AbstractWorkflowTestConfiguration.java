package it.pagopa.pn.deliverypushworkflow.action.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.deliverypushworkflow.action.it.mockbean.*;
import it.pagopa.pn.deliverypushworkflow.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.legalfacts.CustomInstantWriter;
import it.pagopa.pn.deliverypushworkflow.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypushworkflow.legalfacts.LegalFactGeneratorTemplates;
import it.pagopa.pn.deliverypushworkflow.legalfacts.PhysicalAddressWriter;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.datavault.PnDataVaultClientReactive;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.externalregistry.PnExternalRegistriesClientReactive;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClientImpl;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.templatesengine.TemplatesClient;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.templatesengine.TemplatesClientPec;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.userattributes.UserAttributesClient;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.router.deserializer.RouterDeserializer;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.router.deserializer.impl.JsonRouterDeserializer;
import it.pagopa.pn.deliverypushworkflow.middleware.responsehandler.NationalRegistriesResponseHandler;
import it.pagopa.pn.deliverypushworkflow.service.*;
import it.pagopa.pn.deliverypushworkflow.service.impl.NotificationProcessCostServiceImpl;
import it.pagopa.pn.deliverypushworkflow.service.impl.SaveLegalFactsServiceImpl;
import it.pagopa.pn.deliverypushworkflow.utils.PnSendModeUtils;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.List;

public class AbstractWorkflowTestConfiguration {
    static final int SEND_FEE = 100;

    @Bean
    public PnDeliveryPushWorkflowConfigs pnDeliveryPushConfigs() {
        PnDeliveryPushWorkflowConfigs pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushWorkflowConfigs.class);

        // Base configuration
        List<String> pnSendModeList = new ArrayList<>();
        pnSendModeList.add("1970-01-01T00:00:00Z;AAR-DOCUMENTS-PAYMENTS;AAR-DOCUMENTS-PAYMENTS;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION");
        pnSendModeList.add("2023-11-30T23:00:00Z;AAR;AAR;AAR-DOCUMENTS-PAYMENTS;AAR_NOTIFICATION");
        Mockito.when(pnDeliveryPushConfigs.getPnSendMode()).thenReturn(pnSendModeList);
        Mockito.when(pnDeliveryPushConfigs.getPagoPaNotificationBaseCost()).thenReturn(SEND_FEE);

        return pnDeliveryPushConfigs;
    }

    @Bean
    public NotificationProcessCostService notificationProcessCostService(@Lazy TimelineService timelineService,
                                                                         @Lazy PnExternalRegistriesClientReactive pnExternalRegistriesClientReactive,
                                                                         @Lazy PnDeliveryPushWorkflowConfigs cfg) {
        return new NotificationProcessCostServiceImpl(timelineService, pnExternalRegistriesClientReactive, cfg);
    }

    @Bean
    public PnDeliveryClient testPnDeliveryClient() {
        return new PnDeliveryClientMock();
    }

    @Bean
    public PnDataVaultClientReactive testPnDataVaultClient() {
        return new PnDataVaultClientReactiveMock();
    }

    @Bean
    public UserAttributesClient testAddressBook() {
        return new UserAttributesClientMock();
    }

    @Bean
    public PnSafeStorageClient safeStorageTest() {
        return new SafeStorageClientMock();
    }

    @Bean
    public InstantNowSupplier instantNowSupplierTest() {
        return Mockito.mock(InstantNowSupplier.class);
    }

    @Bean
    public LegalFactGenerator legalFactGeneratorTemplatesClient(@Lazy PnSendModeUtils pnSendModeUtils, PnDeliveryPushWorkflowConfigs pnDeliveryPushConfigs) {
        CustomInstantWriter instantWriter = new CustomInstantWriter();
        PhysicalAddressWriter physicalAddressWriter = new PhysicalAddressWriter();
        return new LegalFactGeneratorTemplates(instantWriter, physicalAddressWriter, pnDeliveryPushConfigs, pnSendModeUtils, templatesClient(), templatesClientPec());
    }

    @Bean
    public TemplatesClient templatesClient() {
        return new TemplatesClientMock();
    }

    @Bean
    public TemplatesClientPec templatesClientPec() {
        return new TemplatesClientMockPec();
    }

    @Bean
    public SaveLegalFactsServiceImpl LegalFactsTest(SafeStorageService safeStorageService,
                                                    LegalFactGenerator pdfUtils) {
        return new SaveLegalFactsServiceImpl(pdfUtils, safeStorageService);
    }

    @Bean
    public NationalRegistriesClientMock publicRegistriesMapMock(@Lazy NationalRegistriesResponseHandler nationalRegistriesResponseHandler,
                                                                @Lazy TimelineService timelineService) {
        return new NationalRegistriesClientMock(
                nationalRegistriesResponseHandler,
                timelineService
        );
    }

    @Bean
    public ActionHandlerMock ActionHandlerMock(ActionHandlerRegistry actionHandlerRegistry) {
        return new ActionHandlerMock(actionHandlerRegistry);
    }

    @Bean
    public SchedulerServiceMock schedulerServiceMockMock(@Lazy ActionPoolMock actionPoolMock) {
        return new SchedulerServiceMock(actionPoolMock);
    }


    @Bean
    public PnExternalRegistryClient pnExternalRegistryClientTest() {
        return Mockito.mock(PnExternalRegistryClientImpl.class);
    }

    @Bean
    public ParameterConsumer pnParameterConsumerClientTest() {
        return new AbstractCachedSsmParameterConsumerMock();
    }

    @Bean("jsonRouterDeserializer")
    public RouterDeserializer routerDeserializer() {
        return new JsonRouterDeserializer(new ObjectMapper());
    }

}
