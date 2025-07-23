package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.legalfacts.PdfInfo;
import it.pagopa.pn.deliverypushworkflow.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public interface SaveLegalFactsService {
    PdfInfo sendCreationRequestForAAR(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken);

    String sendCreationRequestForPecDeliveryWorkflowLegalFact(
            List<SendDigitalFeedbackDetailsInt> listFeedbackFromExtChannel,
            NotificationInt notification,
            NotificationRecipientInt recipient,
            EndWorkflowStatus status,
            Instant completionWorkflowDate
    );

    String sendCreationRequestForAnalogDeliveryFailureWorkflowLegalFact(
            NotificationInt notification,
            NotificationRecipientInt recipient,
            EndWorkflowStatus status,
            Instant failureWorkflowDate
    );
    
    Mono<String> sendCreationRequestForNotificationViewedLegalFact(
            NotificationInt notification,
            NotificationRecipientInt recipient,
            DelegateInfoInt delegateInfo,
            Instant timeStamp
    );

    String sendCreationRequestForNotificationCancelledLegalFact(NotificationInt notification, Instant notificationCancellationRequestDate);
}
