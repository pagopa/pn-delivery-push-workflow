package it.pagopa.pn.deliverypushworkflow.action.notificationview;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notificationviewed.NotificationViewedInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ViewNotification {
    private final SaveLegalFactsService legalFactStore;
    private final DocumentCreationRequestService documentCreationRequestService;
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;
    private final AttachmentUtils attachmentUtils;
    private final SafeStorageService safeStorageService;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;
    private final ConfidentialInformationService confidentialInformationService;

    public Mono<Boolean> startVewNotificationProcess(NotificationInt notification,
                                                     NotificationRecipientInt recipient,
                                                     NotificationViewedInt notificationViewed) {
        log.info("Start view notification process - iun={} id={}", notification.getIun(), notificationViewed.getRecipientIndex());
        return checkThatAllAttachmentsArePresent(notification)
                .flatMap(allAttachmentsPresent -> {
                    if (Boolean.FALSE.equals(allAttachmentsPresent)) {
                        auditFlowBlocked(notificationViewed);
                        return Mono.just(false);
                    }

                    Mono<Void> legalFactCreation = notificationViewed.getDelegateInfo() != null
                            ? getDelegateInfoAndHandleLegalFactCreation(notification, recipient, notificationViewed)
                            : handleLegalFactCreation(notification, recipient, notificationViewed);

                    return changeAttachmentRetentionIfNeeded(notification, notificationViewed.getRecipientIndex())
                            .then(legalFactCreation)
                            .thenReturn(true);
                });
    }

    private Mono<Boolean> checkThatAllAttachmentsArePresent(NotificationInt notification) {
        return Flux.fromIterable(extractAttachmentsFromNotification(notification, true))
                .concatMap(document ->
                        safeStorageService.getFile(document.getRef().getKey(), true, false)
                                .thenReturn(true)
                                .onErrorResume(WebClientResponseException.class, ex ->
                                        isAttachmentNotAvailable(ex)
                                                ? Mono.just(false)
                                                : Mono.error(ex)
                                )
                )
                .takeUntil(isPresent -> !isPresent)
                .all(Boolean::booleanValue)
                .doOnNext(allPresent -> {
                    if (!allPresent) {
                        log.warn("View notification blocked, attachment not available in safe storage - iun={}", notification.getIun());
                    }
                });
    }

    private List<NotificationDocumentInt> extractAttachmentsFromNotification(NotificationInt notification, boolean includeF24Metadata) {
        List<NotificationDocumentInt> attachments = new ArrayList<>(notification.getDocuments());

        for(NotificationRecipientInt recipient : notification.getRecipients()) {
            if(recipient.getPayments() != null) {
                recipient.getPayments().forEach(
                        payment -> {
                            if(payment.getPagoPA() != null && payment.getPagoPA().getAttachment() != null) {
                                attachments.add(payment.getPagoPA().getAttachment());
                            }

                            if(includeF24Metadata && payment.getF24() != null && payment.getF24().getMetadataAttachment() != null) {
                                attachments.add(payment.getF24().getMetadataAttachment());
                            }
                        }
                );
            }
        }
        return attachments;
    }

    private boolean isAttachmentNotAvailable(WebClientResponseException ex) {
        return ex.getStatusCode() == HttpStatus.NOT_FOUND || ex.getStatusCode() == HttpStatus.GONE;
    }

    private void auditFlowBlocked(NotificationViewedInt notificationViewed) {
        PnAuditLogEventType type = notificationViewed.getDelegateInfo() != null
                ? PnAuditLogEventType.AUD_NT_VIEW_DEL
                : PnAuditLogEventType.AUD_NT_VIEW_RCP;

        new PnAuditLogBuilder()
                .before(type, "View notification blocked: at least one attachment is not available - iun={} id={}",
                        notificationViewed.getIun(), notificationViewed.getRecipientIndex())
                .iun(notificationViewed.getIun())
                .build()
                .generateWarning("View notification flow blocked due to missing attachment")
                .log();
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
