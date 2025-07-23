package it.pagopa.pn.deliverypushworkflow.action.completionworkflow;

import it.pagopa.pn.deliverypushworkflow.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypushworkflow.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.DigitalDeliveryCreationRequestDetailsInt;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.Optional;

class DigitalDeliveryCreationResponseHandlerTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private SuccessWorkflowHandler successWorkflowHandler;
    @Mock
    private FailureWorkflowHandler failureWorkflowHandler;
    
    private NotificationUtils notificationUtils;
    
    private DigitalDeliveryCreationResponseHandler handler;

    @BeforeEach
    public void setup() {
        notificationUtils = new NotificationUtils();
        handler = new DigitalDeliveryCreationResponseHandler(
                notificationService,
                timelineService,
                successWorkflowHandler,
                failureWorkflowHandler);
    }
    
    @Test
    @ExtendWith(SpringExtension.class)
    void handleDigitalDeliveryCreationResponseSuccess() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();

        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);

        final String legalFactId = "legalFactKey";
        
        DocumentCreationResponseActionDetails actionDetails = DocumentCreationResponseActionDetails.builder()
                .key(legalFactId)
                .documentCreationType(DocumentCreationTypeInt.DIGITAL_DELIVERY)
                .timelineId("testTimelineId")
                .build();

        DigitalDeliveryCreationRequestDetailsInt details = DigitalDeliveryCreationRequestDetailsInt.builder()
                .digitalAddress(LegalDigitalAddressInt.builder().address("test").build())
                .completionWorkflowDate(Instant.now())
                .legalFactId(legalFactId)
                .recIndex(recIndex)
                .endWorkflowStatus(EndWorkflowStatus.SUCCESS)
                .build();
        
        Mockito.when(timelineService.getTimelineElementDetails(Mockito.eq(notification.getIun()),Mockito.eq(actionDetails.getTimelineId()), Mockito.any())).thenReturn(Optional.of(details));
        
        //WHEN
        handler.handleDigitalDeliveryCreationResponse(notification.getIun(), recIndex, actionDetails);
        
        //THEN
        Mockito.verify(successWorkflowHandler).handleSuccessWorkflow(Mockito.eq(notification), Mockito.eq(recIndex), Mockito.any(), Mockito.eq(details));
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void handleDigitalDeliveryCreationResponseFailure() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();

        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);

        final String legalFactId = "legalFactKey";

        DocumentCreationResponseActionDetails actionDetails = DocumentCreationResponseActionDetails.builder()
                .key(legalFactId)
                .documentCreationType(DocumentCreationTypeInt.DIGITAL_DELIVERY)
                .timelineId("testTimelineId")
                .build();

        DigitalDeliveryCreationRequestDetailsInt details = DigitalDeliveryCreationRequestDetailsInt.builder()
                .digitalAddress(LegalDigitalAddressInt.builder().address("test").build())
                .completionWorkflowDate(Instant.now())
                .legalFactId(legalFactId)
                .recIndex(recIndex)
                .endWorkflowStatus(EndWorkflowStatus.FAILURE)
                .build();

        Mockito.when(timelineService.getTimelineElementDetails(Mockito.eq(notification.getIun()),Mockito.eq(actionDetails.getTimelineId()), Mockito.any())).thenReturn(Optional.of(details));

        //WHEN
        handler.handleDigitalDeliveryCreationResponse(notification.getIun(), recIndex, actionDetails);

        //THEN
        Mockito.verify(failureWorkflowHandler).handleFailureWorkflow(Mockito.eq(notification), Mockito.eq(recIndex), Mockito.any(), Mockito.eq(details));
    }

}