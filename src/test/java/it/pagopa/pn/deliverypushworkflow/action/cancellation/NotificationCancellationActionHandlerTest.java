package it.pagopa.pn.deliverypushworkflow.action.cancellation;

import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.service.NotificationCancellationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

class NotificationCancellationActionHandlerTest {

    @Mock
    private NotificationCancellationService notificationCancellationService;

    private NotificationCancellationActionHandler handler;

    @BeforeEach
    void setup() {

        handler = new NotificationCancellationActionHandler(
                notificationCancellationService);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void cancelNotification() {
        //Given
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();

        Mockito.doNothing().when(notificationCancellationService).continueCancellationProcess(notification.getIun());

        //WHEN
        handler.continueCancellationProcess(notification.getIun());

        //THEN
        Mockito.verify(notificationCancellationService).continueCancellationProcess(notification.getIun());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void completeCancellationProcess() {
        //Given
        String iun = "iun";
        String legalFactId = "legalFactId";

        //WHEN
        handler.completeCancellationProcess(iun, legalFactId);

        //THEN
        Mockito.verify(notificationCancellationService).completeCancellationProcess(iun, legalFactId);
    }

}