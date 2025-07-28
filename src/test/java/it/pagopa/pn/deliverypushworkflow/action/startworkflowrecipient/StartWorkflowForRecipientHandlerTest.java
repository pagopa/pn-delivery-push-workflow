package it.pagopa.pn.deliverypushworkflow.action.startworkflowrecipient;

import it.pagopa.pn.deliverypushworkflow.action.details.RecipientsWorkflowDetails;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypushworkflow.action.utils.AarUtils;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static it.pagopa.pn.deliverypushworkflow.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

class StartWorkflowForRecipientHandlerTest {
    @Mock
    private AarUtils aarUtils;
    @Mock
    private NotificationService notificationService;

    
    private StartWorkflowForRecipientHandler handler;

    @ExtendWith(MockitoExtension.class)
    @BeforeEach
    public void setup() {
        handler = new StartWorkflowForRecipientHandler(aarUtils, notificationService);

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void startNotificationWorkflowForRecipient() {
        //GIVEN
        NotificationInt notification = getNotification();

        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);

        //WHEN
        handler.startNotificationWorkflowForRecipient(notification.getIun(), 0, new RecipientsWorkflowDetails("test"));
        
        //THEN
        Mockito.verify(aarUtils).generateAARAndSaveInSafeStorageAndAddTimelineEvent(Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.anyString());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void startNotificationWorkflowForRecipientAarFail() {
        //GIVEN
        NotificationInt notification = getNotification();

        String iun = notification.getIun();
        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);

        doThrow(new PnNotFoundException("Not found","","")).when(aarUtils).generateAARAndSaveInSafeStorageAndAddTimelineEvent(Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.anyString());
        RecipientsWorkflowDetails details = new RecipientsWorkflowDetails("test");
        //WHEN
        assertThrows(PnNotFoundException.class, () -> {
            handler.startNotificationWorkflowForRecipient(iun, 0, details);
        });
    }
    
    private NotificationInt getNotification() {
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("taxId")
                .withInternalId("ANON_"+"taxId")
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();

        return NotificationTestBuilder.builder()
                .withIun("iun")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();
    }
}