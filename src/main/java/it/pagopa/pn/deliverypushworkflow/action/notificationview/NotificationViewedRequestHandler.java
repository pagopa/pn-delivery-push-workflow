package it.pagopa.pn.deliverypushworkflow.action.notificationview;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notificationviewed.NotificationViewedInt;
import it.pagopa.pn.deliverypushworkflow.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypushworkflow.dto.radd.RaddInfo;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class NotificationViewedRequestHandler {

    private final NotificationService notificationService;
    private final TimelineUtils timelineUtils;
    private final NotificationUtils notificationUtils;
    private final ViewNotification viewNotification;


    public NotificationViewedRequestHandler(NotificationService notificationService,
                                            TimelineUtils timelineUtils,
                                            NotificationUtils notificationUtils,
                                            ViewNotification viewNotification) {
        this.notificationService = notificationService;
        this.timelineUtils = timelineUtils;
        this.notificationUtils = notificationUtils;
        this.viewNotification = viewNotification;
    }
    
    //La richiesta proviene da delivery (La visualizzazione potrebbe essere da parte del delegato o da parte del destinatario)
    public void handleViewNotificationDelivery(NotificationViewedInt notificationViewedInt) {
        MDCUtils.addMDCToContextAndExecute(
                handleViewNotification(notificationViewedInt)
        ).block();
    }

    //La richiesta proviene da RADD, visualizzazione da parte del destinatario
    public Mono<Void> handleViewNotificationRadd(NotificationViewedInt notificationViewedInt) {
        return handleViewNotification(notificationViewedInt);
    }

    private Mono<Void> handleViewNotification(NotificationViewedInt notificationViewedInt) {

        return Mono.fromCallable(() -> (
                        timelineUtils.checkIsNotificationCancellationRequested(notificationViewedInt.getIun())))
                .flatMap( isNotificationCancelled -> {
                    if (Boolean.TRUE.equals(isNotificationCancelled)){
                        log.warn("For this notification a cancellation has been requested - iun={} id={}", notificationViewedInt.getIun(), notificationViewedInt.getRecipientIndex());
                        return Mono.empty();
                    } else {
                        return Mono.just(timelineUtils.checkIsNotificationViewed(notificationViewedInt.getIun(), notificationViewedInt.getRecipientIndex()));
                    }
                })
                .flatMap( isNotificationAlreadyViewed -> {

                    //I processi collegati alla visualizzazione di una notifica vengono effettuati solo la prima volta che la stessa viene visualizzata
                    if(Boolean.FALSE.equals(isNotificationAlreadyViewed) ){

                        PnAuditLogEvent logEvent = generateAuditLog(notificationViewedInt.getIun(), notificationViewedInt.getRecipientIndex(), notificationViewedInt.getRaddInfo(), notificationViewedInt.getDelegateInfo());
                        logEvent.log();

                        log.debug("Notification is not already viewed - iun={} id={}", notificationViewedInt.getIun(), notificationViewedInt.getRecipientIndex());

                        return Mono.fromCallable(() -> notificationService.getNotificationByIun(notificationViewedInt.getIun()))
                                .flatMap( notification -> {
                                    NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, notificationViewedInt.getRecipientIndex());
                                    return viewNotification.startVewNotificationProcess(notification, recipient, notificationViewedInt)
                                            .doOnSuccess( x->
                                                    logEvent.generateSuccess().log()
                                            );
                                })
                                .doOnError( err -> logEvent.generateFailure("Exception in View notification iun={} id={}", notificationViewedInt.getIun(), notificationViewedInt.getRecipientIndex(), err).log());
                    } else {
                        log.debug("Notification is already viewed - iun={} id={}", notificationViewedInt.getIun(), notificationViewedInt.getRecipientIndex());
                        return Mono.empty();
                    }
                });
    }

    private PnAuditLogEvent generateAuditLog(String iun, Integer recIndex, RaddInfo raddInfo, DelegateInfoInt delegateInfo ) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        boolean viewedFromDelegate = delegateInfo != null;
        
        PnAuditLogEventType type = viewedFromDelegate ? PnAuditLogEventType.AUD_NT_VIEW_DEL : PnAuditLogEventType.AUD_NT_VIEW_RCP;
        return auditLogBuilder
                .before(type, "View notification - iun={} id={} " +
                                "raddType={} raddTransactionId={} internalDelegateId={} mandateId={}",
                        iun,
                        recIndex,
                        raddInfo != null ? raddInfo.getType() : null,
                        raddInfo != null ? raddInfo.getTransactionId() : null,
                        viewedFromDelegate ? delegateInfo.getInternalId() : null,
                        viewedFromDelegate ? delegateInfo.getMandateId() : null
                )
                .iun(iun)
                .build();
    }

}
