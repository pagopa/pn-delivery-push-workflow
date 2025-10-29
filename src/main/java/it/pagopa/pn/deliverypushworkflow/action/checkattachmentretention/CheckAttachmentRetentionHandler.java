package it.pagopa.pn.deliverypushworkflow.action.checkattachmentretention;

import it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.AnalogFailureWorkflowTimeoutDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.SchedulerService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

@Component
@AllArgsConstructor
@Slf4j
public class CheckAttachmentRetentionHandler {
    private final NotificationService notificationService;
    private final AttachmentUtils attachmentUtils;
    private final PnDeliveryPushWorkflowConfigs configs;
    private final SchedulerService schedulerService;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;

    public void handleCheckAttachmentRetentionBeforeExpiration(String iun, Instant lastActionNotBefore){
        log.debug("Start handleCheckAttachmentRetentionBeforeExpiration - iun={}", iun);
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        boolean notificationHasTriggeredAttachmentRetentionUpdate = notification.getRecipients().stream()
                .allMatch(recipient -> {
                    int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
                    return timelineUtils.hasTimelineTriggeredAttachmentRetentionUpdate(iun, recIndex);
                });
        
        if(! notificationHasTriggeredAttachmentRetentionUpdate ){
            log.info("Notification isn't refined, need to update retention - iun={} ", iun);
            
            //Viene aggiornata la retention degli attachment e inserita una nuova action che, nuovamente, agisca in caso di retention in scadenza
            int attachmentTimeToAddAfterExpiration = (int) configs.getTimeParams().getAttachmentTimeToAddAfterExpiration().toDays();
            scheduleNewCheckAndUpdateAttachmentRetention(iun, lastActionNotBefore, notification, attachmentTimeToAddAfterExpiration);
        }else{
            //Metodo da rimuovere nei prossimi sviluppi, quando il parametro retentionAttachmentDaysAfterDeliveryTimeout sarà impostato != da 0
            handleAnalogDeliveryTimeoutIfPresent(iun, notification, lastActionNotBefore);
        }
    }

    /**
     * Rischedula l'azione per il controllo della retention degli attachment e aggiorna la retention degli attachment.
     * @param iun iun della notifica
     * @param lastActionNotBefore data di schedulazione dell'azione in corso
     * @param notification notifica per la quale si sta aggiornando la retention degli attachment
     * @param daysToAddToRetentionAttachments numero di giorni da aggiungere alla retention degli attachment
     */
    private void scheduleNewCheckAndUpdateAttachmentRetention(String iun, Instant lastActionNotBefore, NotificationInt notification, int daysToAddToRetentionAttachments) {
        scheduleCheckAttachmentRetentionBeforeExpiration(iun, lastActionNotBefore);
        attachmentUtils.changeAttachmentsRetention(notification, daysToAddToRetentionAttachments).blockLast();
    }

    private void handleAnalogDeliveryTimeoutIfPresent(String iun, NotificationInt notification, Instant lastActionNotBefore) {
        Optional<AnalogFailureWorkflowTimeoutDetailsInt> analogFailureWorkflowTimeoutDetailsOpt = checkAndGetLastTimeoutDateFromFailureTimeout(iun);
        // Se non ci sono timeout analogici, si esce dal metodo senza fare nulla
        if (analogFailureWorkflowTimeoutDetailsOpt.isEmpty()) {
            log.info("Notification is already refined, don't need to update retention - iun={} ", iun);
            return;
        }

        AnalogFailureWorkflowTimeoutDetailsInt analogFailureWorkflowTimeoutDetailsWithLastTimeoutDate = analogFailureWorkflowTimeoutDetailsOpt.get();
        int retentionAttachmentDaysAfterDeliveryTimeout = configs.getRetentionAttachmentDaysAfterDeliveryTimeout();

        if(retentionAttachmentDaysAfterDeliveryTimeout == -1) {
            // Se la configurazione è impostata a -1, non si deve fare nulla.
            log.info("RetentionAttachmentDaysAfterDeliveryTimeout is -1, no need to update retention for attachments of iun={}", iun);
        } else if (retentionAttachmentDaysAfterDeliveryTimeout == 0) {
            /* Se la configurazione è impostata a zero, la retention dei documenti dovrà essere riaggiornata e l'action dovrà essere rischedulata. */
            log.info("RetentionAttachmentDaysAfterDeliveryTimeout is 0, scheduling checkAttachmentRetention with attachmentTimeToAddAfterExpiration for iun={}", iun);
            int attachmentTimeToAddAfterExpiration = (int) configs.getTimeParams().getAttachmentTimeToAddAfterExpiration().toDays();
            scheduleNewCheckAndUpdateAttachmentRetention(iun, lastActionNotBefore, notification, attachmentTimeToAddAfterExpiration);
        } else if (retentionAttachmentDaysAfterDeliveryTimeout > 0) {
            /*
                Se la configurazione è maggiore di zero, è necessario contare il numero di giorni passati dall'ultimo elemento di timeout emesso per la notifica.
                Il parametro retentionAttachmentDaysAfterDeliveryTimeout deve essere sottratto dal numero di giorni per verificare se gli attachment sono stati disponibili
                per il periodo di retention previsto.
                Se il risultato della sottrazione è un numero positivo, i documenti devono essere resi disponibili per il numero di giorni risultante e l'azione di controllo rischedulata.
                Se il risultato è 0 o un numero negativo, non sarà necessario aggiornare la retention o rischedulare l'azione di controllo.
            */
            log.info("RetentionAttachmentDaysAfterDeliveryTimeout is {}, checking if it's necessary to reschedule the action and update the retention for iun={}", retentionAttachmentDaysAfterDeliveryTimeout, iun);
            Instant timeoutDate = analogFailureWorkflowTimeoutDetailsWithLastTimeoutDate.getTimeoutDate();
            int daysToAdd = getDaysToAddFromTimeout(timeoutDate, retentionAttachmentDaysAfterDeliveryTimeout);
            if (daysToAdd > 0) {
                log.info("Scheduling and updating checkAttachmentRetention with daysToAdd={} for iun={}", daysToAdd, iun);
                scheduleNewCheckAndUpdateAttachmentRetention(iun, lastActionNotBefore, notification, daysToAdd);
            } else {
                log.info("The timeout element has already used up the expected retention, don't need to update retention - iun={} ", iun);
            }
        }
    }

    private int getDaysToAddFromTimeout(Instant timeoutDate, Integer retentionAttachmentDaysAfterDeliveryTimeout) {
        Duration timeFromTimeout = Duration.between(timeoutDate, Instant.now());
        int daysFromTimeout = ((int) timeFromTimeout.toDays());
        return retentionAttachmentDaysAfterDeliveryTimeout - daysFromTimeout;
    }

    /**
     * Metodo che restituisce il dettaglio di un elemento di timeline con categoria ANALOG_FAILURE_WORKFLOW con la data di timeout più recente
     * @param iun IUN della notifica
     * @return Optional contenente il dettaglio dell'elemento di timeline con la data di timeout più recente, se presente.
     */
    private Optional<AnalogFailureWorkflowTimeoutDetailsInt> checkAndGetLastTimeoutDateFromFailureTimeout(String iun) {
        return timelineService.getTimeline(iun, false)
                .stream()
                .filter(timeline -> timeline.getCategory() == TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW_TIMEOUT)
                .map(timeline -> (AnalogFailureWorkflowTimeoutDetailsInt) timeline.getDetails())
                .max(Comparator.comparing(AnalogFailureWorkflowTimeoutDetailsInt::getTimeoutDate));
    }

    public void scheduleCheckAttachmentRetentionBeforeExpiration(String iun, Instant lastActionNotBefore) {
        Duration attachmentTimeToAddAfterExpiration = configs.getTimeParams().getAttachmentTimeToAddAfterExpiration();
        Duration checkAttachmentTimeBeforeExpiration = configs.getTimeParams().getCheckAttachmentTimeBeforeExpiration();
        log.debug("Start scheduleCheckAttachmentRetentionBeforeExpiration - attachmentDaysToAddAfterExpiration={} checkAttachmentDaysBeforeExpiration={} iun={}",
                attachmentTimeToAddAfterExpiration, checkAttachmentTimeBeforeExpiration, iun);
        
        Duration checkAttachmentTimeToWait = attachmentTimeToAddAfterExpiration.minus(checkAttachmentTimeBeforeExpiration);
        Instant checkAttachmentDate = lastActionNotBefore.plus(checkAttachmentTimeToWait);

        log.info("Scheduling checkAttachmentRetention schedulingDate={} - iun={}", checkAttachmentDate, iun);
        schedulerService.scheduleEvent(iun, checkAttachmentDate, ActionType.CHECK_ATTACHMENT_RETENTION);
    }

}
