package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypushworkflow.action.utils.PaymentUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.cancellation.StatusDetailInt;
import it.pagopa.pn.deliverypushworkflow.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypushworkflow.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypushworkflow.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.PagoPaIntMode;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.EventId;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.NotificationFeePolicy;
import it.pagopa.pn.deliverypushworkflow.service.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.deliverypushworkflow.action.utils.PaymentUtils.handleResponse;
import static it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventId.NOTIFICATION_CANCELLATION_REQUEST;

@Service
@AllArgsConstructor
@Slf4j
public class NotificationCancellationServiceImpl implements NotificationCancellationService {
    public static final int NOTIFICATION_CANCELLED_COST = 0;

    private static final int SECOND_CANCELLATION_STEP = 2; //Da utilizzare per lo step async
    private static final int THIRD_CANCELLATION_STEP = 3;

    private final NotificationService notificationService;
    private final PaperNotificationFailedService paperNotificationFailedService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private AuditLogService auditLogService;
    private NotificationProcessCostService notificationProcessCostService;
    private final SaveLegalFactsService saveLegalFactsService;
    private final DocumentCreationRequestService documentCreationRequestService;

    public void continueCancellationProcess(String iun){
        log.debug("Start continueCancellationProcess - iun={}", iun);
        PnAuditLogEvent logEvent = generateAuditLog(iun, SECOND_CANCELLATION_STEP);

        try {
            // chiedo la cancellazione degli IUV
            notificationService.removeAllNotificationCostsByIun(iun).block();

            NotificationInt notification = notificationService.getNotificationByIun(iun);

            if(NotificationFeePolicy.DELIVERY_MODE.equals(notification.getNotificationFeePolicy()) &&
                    PagoPaIntMode.ASYNC.equals(notification.getPagoPaIntMode())){
                handleUpdateNotificationCost(notification);
            } else {
                log.debug("don't need to update notification cost - iun={}", iun);
            }

            // elimino le righe di paper notification failed
            notification.getRecipients().forEach(recipient ->
                    paperNotificationFailedService.deleteNotificationFailed(recipient.getInternalId(), iun));

            // genero e faccio upload del documento di annullamento
            String legalFactId = saveLegalFactsService.sendCreationRequestForNotificationCancelledLegalFact(notification, getNotificationCancellationRequestDate(iun));

            // salvo l'evento in timeline
            TimelineElementInternal timelineElementInternal = addNotificationCancelledLegalFactTimelineElement(notification, legalFactId);

            // vengono inserite le informazioni della richiesta di creazione del legalFacts a safeStorage
            documentCreationRequestService.addDocumentCreationRequest(legalFactId, notification.getIun(), DocumentCreationTypeInt.NOTIFICATION_CANCELLED, timelineElementInternal.getElementId());

            logEvent.generateSuccess().log();
        } catch (Exception e) {
            logEvent.generateFailure("Error in continueCancellationProcess iun={}", iun, e).log();
            throw e;
        }
    }

    @Override
    public void completeCancellationProcess(String iun, String legalFactId) {
        log.debug("Start completeCancellationProcess - iun={}, legalFactId={}", iun, legalFactId);
        PnAuditLogEvent logEvent = generateAuditLog(iun, THIRD_CANCELLATION_STEP);

        try {
            NotificationInt notification = notificationService.getNotificationByIun(iun);

            // salvo l'evento in timeline
            addCanceledTimelineElement(notification, legalFactId);

            logEvent.generateSuccess().log();
        } catch (Exception e) {
            logEvent.generateFailure("Error in completeCancellationProcess iun={}, legalFactId={}", iun, legalFactId, e).log();
            throw e;
        }
    }

    private Instant getNotificationCancellationRequestDate(String iun) {
        String elementId = NOTIFICATION_CANCELLATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        return timelineService.getTimelineElement(iun, elementId)
                .orElseThrow(() -> new IllegalStateException("Timeline element not found"))
                .getTimestamp();
    }

    private void handleUpdateNotificationCost(NotificationInt notification) {
        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipients = PaymentUtils.getPaymentsInfoFromNotification(notification);
        Instant timestampSendUpdate = Instant.now();

        if( !paymentsInfoForRecipients.isEmpty() ){
            UpdateNotificationCostResponseInt updateNotificationCostResponse = notificationProcessCostService.setNotificationStepCost(
                    NOTIFICATION_CANCELLED_COST,
                    notification.getIun(),
                    paymentsInfoForRecipients,
                    timestampSendUpdate,
                    timestampSendUpdate,
                    UpdateCostPhaseInt.NOTIFICATION_CANCELLED
            ).block();

            if (updateNotificationCostResponse != null && !updateNotificationCostResponse.getUpdateResults().isEmpty()) {
                handleResponse(notification, updateNotificationCostResponse);
            }
        }else {
            log.debug("Don't need to update notification cost, paymentsInfoForRecipients is empty - iun={}", notification.getIun());
        }

    }

    private void addCanceledTimelineElement(NotificationInt notification, String legalFactId) {
        TimelineElementInternal cancelledTimelineElement = timelineUtils.buildCancelledTimelineElement(notification, legalFactId);
        // salvo l'evento in timeline
        timelineService.addTimelineElement(cancelledTimelineElement, notification);
    }

    private TimelineElementInternal addNotificationCancelledLegalFactTimelineElement(NotificationInt notification, String legalFactId) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildNotificationCancelledLegalFactCreationRequest(notification, legalFactId);
        timelineService.addTimelineElement(timelineElementInternal, notification);
        return timelineElementInternal;
    }

    @NotNull
    private PnAuditLogEvent generateAuditLog(String iun, int validationStep) {
        String message = "Notification cancellation step {} of 3.";
        
        return auditLogService.buildAuditLogEvent(
                iun,
                PnAuditLogEventType.AUD_NT_CANCELLED,
                message,
                validationStep
        );
    }
}
