package it.pagopa.pn.deliverypushworkflow.action.choosedeliverymode;

import it.pagopa.pn.deliverypushworkflow.action.digitalworkflow.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypushworkflow.action.digitalworkflow.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypushworkflow.service.NationalRegistriesService;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.utils.FeatureEnabledUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChooseDeliveryModeHandlerTest {

    private DigitalWorkFlowHandler digitalWorkFlowHandler;
    @Mock
    private NationalRegistriesService nationalRegistriesService;
    @Mock
    private ChooseDeliveryModeUtils chooseDeliveryUtils;
    @Mock
    private NotificationService notificationService;
    @Mock
    private DigitalWorkFlowUtils digitalWorkFlowUtils;
    @Mock
    private FeatureEnabledUtils featureEnabledUtils;
    @Mock
    private CourtesyMessageUtils courtesyMessageUtils;

    private ChooseDeliveryModeHandler handler;

    private NotificationUtils notificationUtils;

    private PnDeliveryPushWorkflowConfigs cfg;

    @BeforeEach
    public void setup() {
        digitalWorkFlowHandler = Mockito.mock(DigitalWorkFlowHandler.class);

        cfg = mock(PnDeliveryPushWorkflowConfigs.class);
        FeatureEnabledUtils featureEnabledUtils = new FeatureEnabledUtils(cfg);
        handler = new ChooseDeliveryModeHandler(digitalWorkFlowHandler, nationalRegistriesService,
                chooseDeliveryUtils, notificationService, featureEnabledUtils, courtesyMessageUtils);
        notificationUtils= new NotificationUtils();
    }

    @Test
    void chooseDeliveryTypeOnNewWorkFlow() {
        when(cfg.getPfNewWorkflowStart()).thenReturn("1099-03-31T23:00:00Z");
        when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");

        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);

        handler.chooseDeliveryTypeAndStartWorkflow(notification.getIun(), recIndex);

        Mockito.verifyNoInteractions(digitalWorkFlowHandler);
        Mockito.verifyNoInteractions(digitalWorkFlowUtils);
        Mockito.verify(nationalRegistriesService, times(1)).sendRequestForGetDigitalGeneralAddress(Mockito.any(NotificationInt.class), Mockito.anyInt(),
                Mockito.any(ContactPhaseInt.class), Mockito.anyInt(), Mockito.any());
    }


    @Test
    void chooseDeliveryTypeAndStartWorkflowPlatformAddress() {
        when(cfg.getPfNewWorkflowStart()).thenReturn("2099-03-31T23:00:00Z");
        when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);

        Mockito.when(chooseDeliveryUtils.retrievePlatformAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(Optional.of(recipient.getDigitalDomicile()));

        handler.chooseDeliveryTypeAndStartWorkflow(notification.getIun(), recIndex);

        Mockito.verify(chooseDeliveryUtils, times(0)).retrieveSpecialAddress(Mockito.any(NotificationInt.class), Mockito.anyInt());
        Mockito.verify(digitalWorkFlowHandler, times(1))
                .startDigitalWorkflow(any(NotificationInt.class), eq(recipient.getDigitalDomicile()), eq(DigitalAddressSourceInt.PLATFORM), anyInt());
    }

    @Test
    void chooseDeliveryTypeAndStartWorkflowSpecial() {
        when(cfg.getPfNewWorkflowStart()).thenReturn("2099-03-31T23:00:00Z");
        when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");
        //GIVEN
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);

        Mockito.when(chooseDeliveryUtils.retrievePlatformAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(Optional.empty());
        Mockito.when(chooseDeliveryUtils.retrieveSpecialAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(recipient.getDigitalDomicile());

        handler.chooseDeliveryTypeAndStartWorkflow(notification.getIun(), recIndex);

        Mockito.verify(digitalWorkFlowHandler, times(1))
                .startDigitalWorkflow(any(NotificationInt.class), eq(recipient.getDigitalDomicile()), eq(DigitalAddressSourceInt.SPECIAL), anyInt());

    }

    @Test
    void chooseDeliveryTypeAndStartWorkflowGeneral() {
        when(cfg.getPfNewWorkflowStart()).thenReturn("2099-03-31T23:00:00Z");
        when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");

        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);

        Mockito.when(chooseDeliveryUtils.retrievePlatformAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(Optional.empty());
        Mockito.when(chooseDeliveryUtils.retrieveSpecialAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(null);

        handler.chooseDeliveryTypeAndStartWorkflow(notification.getIun(), recIndex);

        Mockito.verifyNoInteractions(digitalWorkFlowHandler);
        Mockito.verify(nationalRegistriesService, times(1)).sendRequestForGetDigitalGeneralAddress(Mockito.any(NotificationInt.class), Mockito.anyInt(),
                Mockito.any(ContactPhaseInt.class), Mockito.anyInt(), Mockito.any());
    }

    @Test
    void handleGeneralAddressResponseDigital() {

        NationalRegistriesResponse response = NationalRegistriesResponse.builder()
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("Via nuova")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build()).build();

        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        
        //WHEN
        handler.handleGeneralAddressResponse(response, notification, recIndex);

        //THEN
        ArgumentCaptor<DigitalAddressSourceInt> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSourceInt.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowHandler).startDigitalWorkflow(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                digitalAddressSourceCaptor.capture(), Mockito.anyInt());
        Assertions.assertEquals(DigitalAddressSourceInt.GENERAL, digitalAddressSourceCaptor.getValue());

        Mockito.verify(chooseDeliveryUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        Assertions.assertTrue(isAvailableCaptor.getValue());
        Assertions.assertEquals(DigitalAddressSourceInt.GENERAL, digitalAddressSourceCaptor.getValue());
    }

    @Test
    void handleGeneralAddressResponseAnalog() {
        when(cfg.getPfNewWorkflowStart()).thenReturn("2099-03-31T23:00:00Z");
        when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");
        //GIVEN
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        NationalRegistriesResponse response = NationalRegistriesResponse.builder()
                .digitalAddress(null).build();

        //WHEN
        handler.handleGeneralAddressResponse(response, notification, recIndex);

        //THEN
        ArgumentCaptor<DigitalAddressSourceInt> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSourceInt.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(chooseDeliveryUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        Assertions.assertFalse(isAvailableCaptor.getValue());
        Assertions.assertEquals(DigitalAddressSourceInt.GENERAL, digitalAddressSourceCaptor.getValue());

        // il ramo analogico esegue solo il dispatch delle azioni di cortesia
        Mockito.verify(courtesyMessageUtils).scheduleCourtesyMessagesActions(notification, recIndex, DeliveryModeInt.ANALOG);
    }

    @Test
    void handleGeneralAddressAnalogWorkflow() {
        when(cfg.getPfNewWorkflowStart()).thenReturn("1099-03-31T23:00:00Z");
        when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");
        //GIVEN
        NationalRegistriesResponse response = NationalRegistriesResponse.builder()
                .digitalAddress(null).build();

        NotificationInt notification = getNotification();

        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        when(chooseDeliveryUtils.retrieveSpecialAddress(notification, recIndex)).thenReturn(null);
        when(chooseDeliveryUtils.retrievePlatformAddress(notification, recIndex)).thenReturn(Optional.empty());

        //WHEN
        handler.handleGeneralAddressResponse(response, notification, recIndex);

        verify(chooseDeliveryUtils, times(1)).addAvailabilitySourceToTimeline(anyInt(), any(NotificationInt.class), eq(DigitalAddressSourceInt.GENERAL), eq(false));
        verifyNoInteractions(digitalWorkFlowHandler);
        verify(courtesyMessageUtils, times(1)).scheduleCourtesyMessagesActions(notification, recIndex, DeliveryModeInt.ANALOG);
    }

    @Test
    void handleGeneralAddressResponsePlatformFoundNewWorkflow() {
        when(cfg.getPfNewWorkflowStart()).thenReturn("1099-03-31T23:00:00Z");
        when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");
        //GIVEN
        NationalRegistriesResponse response = NationalRegistriesResponse.builder()
                .digitalAddress(null).build();

        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        when(chooseDeliveryUtils.retrieveSpecialAddress(notification, recIndex)).thenReturn(null);
        when(chooseDeliveryUtils.retrievePlatformAddress(notification, recIndex)).thenReturn(Optional.of(recipient.getDigitalDomicile()));

        //WHEN
        handler.handleGeneralAddressResponse(response, notification, recIndex);
        verify(chooseDeliveryUtils, times(1)).addAvailabilitySourceToTimeline(anyInt(), any(NotificationInt.class), eq(DigitalAddressSourceInt.GENERAL), eq(false));
        Mockito.verify(digitalWorkFlowHandler).startDigitalWorkflow(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                Mockito.any(DigitalAddressSourceInt.class), Mockito.anyInt());
        verify(chooseDeliveryUtils, times(0)).addScheduleAnalogWorkflowToTimeline(eq(recIndex), eq(notification), any(Instant.class));
        verifyNoInteractions(courtesyMessageUtils);

    }

    @Test
    void handleGeneralAddressResponseSpecialFoundNewWorkflow() {
        when(cfg.getPfNewWorkflowStart()).thenReturn("1099-03-31T23:00:00Z");
        when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");
        //GIVEN
        NationalRegistriesResponse response = NationalRegistriesResponse.builder()
                .digitalAddress(null).build();

        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        when(chooseDeliveryUtils.retrieveSpecialAddress(notification, recIndex)).thenReturn(recipient.getDigitalDomicile());

        //WHEN
        handler.handleGeneralAddressResponse(response, notification, recIndex);
        verify(chooseDeliveryUtils, times(1)).addAvailabilitySourceToTimeline(anyInt(), any(NotificationInt.class), eq(DigitalAddressSourceInt.GENERAL), eq(false));
        Mockito.verify(digitalWorkFlowHandler).startDigitalWorkflow(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                Mockito.any(DigitalAddressSourceInt.class), Mockito.anyInt());
        verify(chooseDeliveryUtils, times(0)).retrievePlatformAddress(any(NotificationInt.class),anyInt());
        verify(chooseDeliveryUtils, times(0)).addScheduleAnalogWorkflowToTimeline(eq(recIndex), eq(notification), any(Instant.class));
        verifyNoInteractions(courtesyMessageUtils);

    }

    private NotificationInt getNotification() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .sentAt(Instant.now())
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }
}