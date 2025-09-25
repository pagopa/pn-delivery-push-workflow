package it.pagopa.pn.deliverypushworkflow.action.notificationview;

import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notificationviewed.NotificationViewedInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypushworkflow.service.DocumentCreationRequestService;
import it.pagopa.pn.deliverypushworkflow.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class ViewNotification {
    private final SaveLegalFactsService legalFactStore;
    private final DocumentCreationRequestService documentCreationRequestService;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;
    private final AttachmentUtils attachmentUtils;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;
    private final ConfidentialInformationService confidentialInformationService;

    public Mono<Void> startVewNotificationProcess(NotificationInt notification,
                                                  NotificationRecipientInt recipient,
                                                  NotificationViewedInt notificationViewed) {
        log.info("Start view notification process - iun={} id={}", notification.getIun(), notificationViewed.getRecipientIndex());
        if(notificationViewed.getDelegateInfo() != null){
            return changeAttachmentRetentionIfNeeded(notification, notificationViewed.getRecipientIndex())
                    .then(getDelegateInfoAndHandleLegalFactCreation(notification, recipient, notificationViewed));
        } else {
            return changeAttachmentRetentionIfNeeded(notification, notificationViewed.getRecipientIndex())
                    .then(handleLegalFactCreation(notification, recipient, notificationViewed));
        }
    }

    private Mono<Void> changeAttachmentRetentionIfNeeded(NotificationInt notification, Integer recIndex){

        if (
                timelineUtils.checkIsNotificationRefined(notification.getIun(), recIndex) ||
                        timelineUtils.checkIsRecipientDeceased(notification.getIun(), recIndex) ||
                        timelineUtils.checkIsNotificationFailureTimeout(notification.getIun(), recIndex)
        ) {
            log.info("No need to change attachment retention, notification is already REFINED or delivery FAILED for TIMEOUT or recipient is DECEASED iun={} recIndex={}", notification.getIun(), recIndex);
            return Mono.empty();
        }

        return attachmentUtils.changeAttachmentsRetention(notification, pnDeliveryPushWorkflowConfigs.getRetentionAttachmentDaysAfterRefinement()).collectList().then();
    }

    private Mono<Void> getDelegateInfoAndHandleLegalFactCreation(NotificationInt notification, NotificationRecipientInt recipient, NotificationViewedInt notificationViewed) {
        return confidentialInformationService.getRecipientInformationByInternalId(notificationViewed.getDelegateInfo().getInternalId())
                .doOnSuccess( baseRecipientDto -> log.info("Completed getBaseRecipientDtoIntMono - iun={} id={} taxId={}" , notification.getIun(), notificationViewed.getRecipientIndex(), LogUtils.maskTaxId(baseRecipientDto.getTaxId())))
                .flatMap(delegateDtoInt -> {
                    if(delegateDtoInt != null && StringUtils.hasText(delegateDtoInt.getDenomination())) {
                        return Mono.just(delegateDtoInt);
                    } else {
                        return retrieveDelegateInfoMissingDenomination(notification, notificationViewed, delegateDtoInt);
                    }
                })
                .doOnNext(baseRecipientDtoInt -> {
                    notificationViewed.getDelegateInfo().setDenomination(baseRecipientDtoInt.getDenomination());
                    notificationViewed.getDelegateInfo().setTaxId(baseRecipientDtoInt.getTaxId());
                })
                .then(handleLegalFactCreation(notification, recipient, notificationViewed));
    }

    private Mono<BaseRecipientDtoInt> retrieveDelegateInfoMissingDenomination(NotificationInt notification, NotificationViewedInt notificationViewed, BaseRecipientDtoInt delegateDtoInt) {
        /*
        La denominazione potrebbe non essere valorizzata se il delegato è stato creato tramite AppIO
        Dunque in questo caso provo a recuperarla dai dati anonimizzati persistiti su data-vault in fase di creazione delega.
        Il taxCode invece in delegateDtoInt mi aspetto sia sempre valorizzato, dunque non lo sovrascrivo.
        */
        log.info("Delegate info missing denomination - iun={} id={} internalId={}", notification.getIun(), notificationViewed.getRecipientIndex(), notificationViewed.getDelegateInfo().getInternalId());
        return confidentialInformationService.getDelegateInformationByMandateId(notificationViewed.getDelegateInfo().getMandateId(), notificationViewed.getDelegateInfo().getDelegateType())
                .doOnNext(delegateInfo -> delegateInfo.setTaxId(delegateDtoInt.getTaxId()));
    }

    @NotNull
    private Mono<Void> handleLegalFactCreation(NotificationInt notification, NotificationRecipientInt recipient, NotificationViewedInt notificationViewed) {
        return legalFactStore.sendCreationRequestForNotificationViewedLegalFact(notification, recipient, notificationViewed.getDelegateInfo(), notificationViewed.getViewedDate())
                .doOnSuccess( legalFactId -> log.info("Completed sendCreationRequestForNotificationViewedLegalFact legalFactId={} - iun={} id={}", legalFactId, notification.getIun(), notificationViewed.getRecipientIndex()))
                .flatMap(legalFactId ->
                        Mono.fromRunnable( () -> {
                            TimelineElementInternal timelineElementInternal = timelineUtils.buildNotificationViewedLegalFactCreationRequestTimelineElement(notification, legalFactId, notificationViewed);
                            addTimelineElement( timelineElementInternal , notification);

                            //Vengono inserite le informazioni della richiesta di creazione del legalFacts a safeStorage
                            documentCreationRequestService.addDocumentCreationRequest(legalFactId, notification.getIun(), notificationViewed.getRecipientIndex(), DocumentCreationTypeInt.RECIPIENT_ACCESS, timelineElementInternal.getElementId());
                        })
                );
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
