package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkValidationDetails;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushWorkflowGenericException;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.api.TimelineControllerApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationStatus;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReworkHandlerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private TimelineControllerApi timelineControllerApi;

    private ReworkHandler reworkHandler;

    @BeforeEach
    void setup() {
        reworkHandler = new ReworkHandler(notificationService, timelineControllerApi);
    }

    @Test
    void handleReworkCompletesSuccessfullyForMonoAcceptedStatus() {
        String iun = "IUN_OK";
        int recIndex = 1;
        NotificationInt notification = NotificationInt.builder()
                .iun(iun)
                .recipients(Collections.singletonList(NotificationRecipientInt.builder().build()))
                .sentAt(Instant.now())
                .build();

        Action action = Action.builder()
                .iun(iun)
                .recipientIndex(recIndex)
                .details(new NotificationReworkValidationDetails("1", "1", "PCRETRY_0", "1","FINAL_STATUS"))
                .build();

        NotificationHistoryResponse response = new NotificationHistoryResponse();
        response.setNotificationStatus(NotificationStatus.VIEWED);

        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(timelineControllerApi.getTimelineAndStatusHistory(iun, 1, notification.getSentAt())).thenReturn(response);

        reworkHandler.handleRework(action);
        Mockito.verify(notificationService).getNotificationByIun(iun);
        Mockito.verify(timelineControllerApi).getTimelineAndStatusHistory(iun, 1, notification.getSentAt());
    }

    @Test
    void handleReworkThrowsWhenRecipientIndexIsInvalid() {
        String iun = "IUN_ERR";
        int recIndex = 2;
        NotificationInt notification = NotificationInt.builder()
                .iun(iun)
                .recipients(Collections.singletonList(NotificationRecipientInt.builder().build()))
                .sentAt(Instant.now())
                .build();

        Action action = Action.builder()
                .iun(iun)
                .recipientIndex(recIndex)
                .details(new NotificationReworkValidationDetails("1", "1", "PCRETRY_0", "2","FINAL_STATUS"))
                .build();

        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);

        assertThrows(PnDeliveryPushWorkflowGenericException.class, () -> reworkHandler.handleRework(action));
    }

    @Test
    void handleReworkThrowsWhenMonoStatusIsNotAccepted() {
        String iun = "IUN_NOT_ACCEPTED";
        int recIndex = 1;
        NotificationInt notification = NotificationInt.builder()
                .iun(iun)
                .recipients(Collections.singletonList(NotificationRecipientInt.builder().build()))
                .sentAt(Instant.now())
                .build();

        Action action = Action.builder()
                .iun(iun)
                .recipientIndex(recIndex)
                .details(new NotificationReworkValidationDetails("1", "1", "PCRETRY_0", "1","FINAL_STATUS"))
                .build();

        NotificationHistoryResponse response = new NotificationHistoryResponse();
        response.setNotificationStatus(NotificationStatus.DELIVERING);

        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(timelineControllerApi.getTimelineAndStatusHistory(iun, 1, notification.getSentAt())).thenReturn(response);

        assertThrows(PnDeliveryPushWorkflowGenericException.class, () -> reworkHandler.handleRework(action));
    }

    @Test
    void handleReworkThrowsWhenMultiStatusIsNotAccepted() {
        String iun = "IUN_MULTI";
        int recIndex = 2;
        NotificationInt notification = NotificationInt.builder()
                .iun(iun)
                .recipients(List.of(NotificationRecipientInt.builder().build(), NotificationRecipientInt.builder().build()))
                .sentAt(Instant.now())
                .build();

        Action action = Action.builder()
                .iun(iun)
                .recipientIndex(recIndex)
                .details(new NotificationReworkValidationDetails("1", "1", "PCRETRY_0", "2","FINAL_STATUS"))
                .build();

        NotificationHistoryResponse response = new NotificationHistoryResponse();
        response.setNotificationStatus(NotificationStatus.CANCELLED);

        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(timelineControllerApi.getTimelineAndStatusHistory(iun, 2, notification.getSentAt())).thenReturn(response);

        assertThrows(PnDeliveryPushWorkflowGenericException.class, () -> reworkHandler.handleRework(action));
    }
}