package it.pagopa.pn.deliverypushworkflow.service.mapper;

import it.pagopa.pn.api.dto.events.EndWorkflowStatus;
import it.pagopa.pn.commons.utils.FileUtils;

import it.pagopa.pn.deliverypushworkflow.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypushworkflow.dto.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypushworkflow.dto.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypushworkflow.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.templatesengine.model.*;
import it.pagopa.pn.deliverypushworkflow.legalfacts.CustomInstantWriter;
import it.pagopa.pn.deliverypushworkflow.legalfacts.PhysicalAddressWriter;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class TemplatesEngineMapper {

    private TemplatesEngineMapper() {
    }

    public static NotificationAarRaddAltDto notificationAARRADDalt(NotificationInt notification,
                                                                   NotificationRecipientInt recipient,
                                                                   String qrCodeQuickAccessUrlAarDetail,
                                                                   String piattaformaNotificheURL,
                                                                   String accessUrlLabel,
                                                                   String accessLink,
                                                                   String accessLinkLabel,
                                                                   String perfezionamentoLink,
                                                                   String perfezionamentoLinkLabel,
                                                                   String raddPhoneNumber,
                                                                   String senderLogoBase64) {
        AarRaddAltSenderDto sender = new AarRaddAltSenderDto()
                .paDenomination(notification.getSender().getPaDenomination());

        AarRaddAltNotificationDto altNotification = new AarRaddAltNotificationDto()
                .iun(notification.getIun())
                .subject(notification.getSubject())
                .sender(sender);

        AarRaddAltRecipientDto aarRecipient = new AarRaddAltRecipientDto()
                .recipientType(recipient.getRecipientType().getValue())
                .taxId(recipient.getTaxId())
                .denomination(recipient.getDenomination());

        return new NotificationAarRaddAltDto()
                .notification(altNotification)
                .recipient(aarRecipient)
                .piattaformaNotificheURL(piattaformaNotificheURL)
                .piattaformaNotificheURLLabel(accessUrlLabel)
                .sendURL(accessLink)
                .sendURLLAbel(accessLinkLabel)
                .perfezionamentoURL(perfezionamentoLink)
                .perfezionamentoURLLabel(perfezionamentoLinkLabel)
                .qrCodeQuickAccessLink(qrCodeQuickAccessUrlAarDetail)
                .raddPhoneNumber(raddPhoneNumber)
                .senderLogoBase64(senderLogoBase64);
    }

    public static NotificationAarDto notificationAAR(NotificationInt notification,
                                                     NotificationRecipientInt recipient,
                                                     String qrCodeQuickAccessUrlAarDetail,
                                                     String piattaformaNotificheURL,
                                                     String accessUrlLabel,
                                                     String perfezionamentoLink,
                                                     String perfezionamentoLinkLabel,
                                                     String senderLogoBase64) {
        AarSenderDto sender = new AarSenderDto()
                .paDenomination(notification.getSender().getPaDenomination());

        AarNotificationDto aarNotification = new AarNotificationDto()
                .iun(notification.getIun())
                .subject(notification.getSubject())
                .sender(sender);

        AarRecipientDto aarRecipient = new AarRecipientDto()
                .recipientType(recipient.getRecipientType().getValue())
                .taxId(recipient.getTaxId());

        return new NotificationAarDto()
                .notification(aarNotification)
                .recipient(aarRecipient)
                .piattaformaNotificheURL(piattaformaNotificheURL)
                .piattaformaNotificheURLLabel(accessUrlLabel)
                .perfezionamentoURL(perfezionamentoLink)
                .perfezionamentoURLLabel(perfezionamentoLinkLabel)
                .qrCodeQuickAccessLink(qrCodeQuickAccessUrlAarDetail)
                .senderLogoBase64(senderLogoBase64);
    }

    public static NotificationAarForSmsDto notificationAarForSms(NotificationInt notification) {
        AarForSmsSenderDto sender = new AarForSmsSenderDto()
                .paDenomination(notification.getSender().getPaDenomination());

        AarForSmsNotificationDto aarForSmsNotification = new AarForSmsNotificationDto()
                .iun(notification.getIun())
                .sender(sender);

        return new NotificationAarForSmsDto()
                .notification(aarForSmsNotification);
    }

    public static NotificationAarForPecDto notificationAarForPec(NotificationInt notification,
                                                              NotificationRecipientInt recipient,
                                                              String quickAccessLink,
                                                              String perfezionamentoLink,
                                                              String faqSendURL,
                                                              String piattaformaNotificheURL,
                                                              String recipientTypeForHTMLTemplate) {
        AarForPecSenderDto sender = new AarForPecSenderDto()
                .paDenomination(notification.getSender().getPaDenomination());

        AarForPecNotificationDto pecNotification = new AarForPecNotificationDto()
                .iun(notification.getIun())
                .subject(notification.getSubject())
                .sender(sender);

        AarForPecRecipientDto aarForPecRecipient = new AarForPecRecipientDto()
                .taxId(recipient.getTaxId());

        return new NotificationAarForPecDto()
                .perfezionamentoURL(perfezionamentoLink)
                .quickAccessLink(quickAccessLink)
                .pnFaqSendURL(faqSendURL)
                .piattaformaNotificheURL(piattaformaNotificheURL)
                .notification(pecNotification)
                .recipient(aarForPecRecipient)
                .recipientType(recipientTypeForHTMLTemplate);
    }

    public static NotificationAarForEmailDto notificationAarForEmail(NotificationInt notification,
                                                                  String perfezionamentoLink,
                                                                  String quickAccessLink,
                                                                  String faqSendURL,
                                                                  String piattaformaNotificheURL) {
        AarForEmailSenderDto sender = new AarForEmailSenderDto()
                .paDenomination(notification.getSender().getPaDenomination());

        AarForEmailNotificationDto aarForEmailNotification = new AarForEmailNotificationDto()
                .iun(notification.getIun())
                .sender(sender);

        return new NotificationAarForEmailDto()
                .perfezionamentoURL(perfezionamentoLink)
                .quickAccessLink(quickAccessLink)
                .pnFaqSendURL(faqSendURL)
                .piattaformaNotificheURL(piattaformaNotificheURL)
                .notification(aarForEmailNotification);
    }

    public static NotificationAarForSubjectDto notificationAARSubject(NotificationInt notification) {
        AarForSubjectSenderDto sender = new AarForSubjectSenderDto()
                .paDenomination(notification.getSender().getPaDenomination());

        AarForSubjectNotificationDto aarForSubjectNotification = new AarForSubjectNotificationDto()
                .sender(sender)
                .iun(notification.getIun());

        return new NotificationAarForSubjectDto()
                .notification(aarForSubjectNotification);
    }

    public static NotificationCancelledLegalFactDto cancelledLegalFact(NotificationInt notification,
                                                                    Instant notificationCancellationRequestDate,
                                                                    CustomInstantWriter instantWriter) {
        NotificationCancelledSenderDto sender = new NotificationCancelledSenderDto()
                .paDenomination(notification.getSender().getPaDenomination());

        List<NotificationCancelledRecipientDto> recipients = notification.getRecipients()
                .stream()
                .map(recipientInt -> new NotificationCancelledRecipientDto()
                        .denomination(recipientInt.getDenomination())
                        .taxId(recipientInt.getTaxId()))
                .toList();

        NotificationCancelledNotificationDto notificationCancelledNotification = new NotificationCancelledNotificationDto()
                .iun(notification.getIun())
                .recipients(recipients)
                .sender(sender);

        return new NotificationCancelledLegalFactDto()
                .notificationCancelledDate(instantWriter.instantToDate(notificationCancellationRequestDate))
                .notification(notificationCancelledNotification);
    }

    public static AnalogDeliveryWorkflowFailureLegalFactDto analogDeliveryWorkflowFailureLegalFact(NotificationInt notification,
                                                                                                NotificationRecipientInt recipient,
                                                                                                Instant failureWorkflowDate,
                                                                                                CustomInstantWriter instantWriter) {
        AnalogDeliveryWorkflowFailureRecipientDto analogDeliveryWorkflowFailureRecipient = new AnalogDeliveryWorkflowFailureRecipientDto()
                .denomination(recipient.getDenomination())
                .taxId(recipient.getTaxId());

        return new AnalogDeliveryWorkflowFailureLegalFactDto()
                .iun(notification.getIun())
                .recipient(analogDeliveryWorkflowFailureRecipient)
                .endWorkflowDate(instantWriter.instantToDate(failureWorkflowDate, true))
                .endWorkflowTime(instantWriter.instantToTime(failureWorkflowDate));
    }

    public static PecDeliveryWorkflowLegalFactDto pecDeliveryWorkflowLegalFact(List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList,
                                                                            NotificationInt notification,
                                                                            NotificationRecipientInt recipient,
                                                                            EndWorkflowStatus status,
                                                                            Instant completionWorkflowDate,
                                                                            CustomInstantWriter instantWriter) {
        List<PecDeliveryWorkflowDeliveryDto> pecDeliveries = feedbackFromExtChannelList.stream()
                .map(feedbackFromExtChannel -> {
                    ResponseStatusInt sentPecStatus = feedbackFromExtChannel.getResponseStatus();
                    Instant notificationDate = feedbackFromExtChannel.getNotificationDate();
                    String addressSource = Optional.ofNullable(feedbackFromExtChannel.getDigitalAddressSource())
                            .map(DigitalAddressSourceInt::getValue)
                            .orElse(null);
                    return new PecDeliveryWorkflowDeliveryDto()
                            .denomination(recipient.getDenomination())
                            .taxId(recipient.getTaxId())
                            .address(feedbackFromExtChannel.getDigitalAddress().getAddress())
                            .addressSource(addressSource)
                            .type(feedbackFromExtChannel.getDigitalAddress().getType().getValue())
                            .responseDate(instantWriter.instantToDate(notificationDate))
                            .ok(ResponseStatusInt.OK.equals(sentPecStatus));
                })
                .sorted(Comparator.comparing(PecDeliveryWorkflowDeliveryDto::getResponseDate))
                .toList();

        return new PecDeliveryWorkflowLegalFactDto()
                .iun(notification.getIun())
                .endWorkflowStatus(status.toString())
                .deliveries(pecDeliveries)
                .endWorkflowDate(instantWriter.instantToDate(completionWorkflowDate));
    }

    public static NotificationViewedLegalFactDto notificationViewedLegalFact(String iun,
                                                                          NotificationRecipientInt recipient,
                                                                          DelegateInfoInt delegateInfo,
                                                                          Instant timeStamp,
                                                                          CustomInstantWriter instantWriter) {
        NotificationViewedRecipientDto notificationViewedRecipient = new NotificationViewedRecipientDto()
                .denomination(recipient.getDenomination())
                .taxId(recipient.getTaxId());

        return new NotificationViewedLegalFactDto()
                .recipient(notificationViewedRecipient)
                .iun(iun)
                .delegate(notificationViewedDelegate(delegateInfo))
                .when(instantWriter.instantToDate(timeStamp));
    }

    private static NotificationViewedDelegateDto notificationViewedDelegate(DelegateInfoInt delegateInfo) {
        return delegateInfo != null ?
                new NotificationViewedDelegateDto()
                        .denomination(delegateInfo.getDenomination())
                        .taxId(delegateInfo.getTaxId())
                : null;
    }

    public static NotificationReceivedLegalFactDto notificationReceivedLegalFact(NotificationInt notification,
                                                                              PhysicalAddressWriter physicalAddressWriter,
                                                                              CustomInstantWriter instantWriter) {
        String physicalAddressAndDenomination;
        List<NotificationRecipientInt> recipients = Optional.of(notification)
                .map(NotificationInt::getRecipients)
                .orElse(new ArrayList<>());

        List<NotificationReceivedRecipientDto> receivedRecipients = new ArrayList<>();
        for (var recipientInt : recipients) {
            String denomination = recipientInt.getDenomination();
            physicalAddressAndDenomination = physicalAddressWriter.nullSafePhysicalAddressToString(
                    recipientInt.getPhysicalAddress(), denomination, "<br/>");
            NotificationReceivedRecipientDto notificationReceivedNotification =
                    notificationReceivedNotification(physicalAddressAndDenomination, recipientInt);
            receivedRecipients.add(notificationReceivedNotification);
        }

        NotificationReceivedNotificationDto notificationReceivedNotification = new NotificationReceivedNotificationDto()
                .iun(notification.getIun())
                .recipients(receivedRecipients)
                .sender(sender(notification));

        return new NotificationReceivedLegalFactDto()
                .sendDate(instantWriter.instantToDate(notification.getSentAt()))
                .subject(notification.getSubject())
                .notification(notificationReceivedNotification)
                .digests(extractNotificationAttachmentDigests(notification));
    }

    private static NotificationReceivedRecipientDto notificationReceivedNotification(String physicalAddressAndDenomination,
                                                                                  NotificationRecipientInt recipientInt) {
        return recipientInt != null ?
                new NotificationReceivedRecipientDto()
                        .physicalAddressAndDenomination(physicalAddressAndDenomination)
                        .denomination(recipientInt.getDenomination())
                        .taxId(recipientInt.getTaxId())
                        .digitalDomicile(digitalDomicile(recipientInt.getDigitalDomicile())) : null;
    }

    private static NotificationReceivedDigitalDomicileDto digitalDomicile(LegalDigitalAddressInt domicile) {
        return domicile != null ? new NotificationReceivedDigitalDomicileDto().address(domicile.getAddress()) : null;
    }

    private static NotificationReceivedSenderDto sender(NotificationInt notification) {
        var senderInt = Optional.of(notification).map(NotificationInt::getSender).orElse(null);
        return senderInt != null ?
                new NotificationReceivedSenderDto()
                        .paDenomination(senderInt.getPaDenomination())
                        .paTaxId(senderInt.getPaTaxId())
                : null;
    }

    /**
     * Extracts the SHA-256 digests of the attachments related to a notification.
     *
     * @param notification the {@link NotificationInt} object containing the details of the notification,
     *                     including its attached documents and recipients with payment information.
     * @return a {@link List} of {@link String} representing the SHA-256 digests (in hexadecimal uppercase)
     * of all relevant attachments from the notification.
     */
    private static List<String> extractNotificationAttachmentDigests(NotificationInt notification) {
        List<String> digests = new ArrayList<>();
        // - Documents digests
        for (NotificationDocumentInt attachment : notification.getDocuments()) {
            digests.add(FileUtils.convertBase64toHexUppercase(attachment.getDigests().getSha256()));
        }
        // F24 digests
        for (NotificationRecipientInt recipient : notification.getRecipients()) {
            //add digests for v21
            addDigestsForMultiPayments(recipient.getPayments(), digests);
        }
        return digests;
    }

    /**
     * Adds the SHA-256 digests of the attachments related to the payments made by the recipient.
     *
     * @param payments a {@link List} of {@link NotificationPaymentInfoInt} objects representing the payments
     *                 made by the recipient, potentially containing attachments.
     * @param digests  a {@link List} of {@link String} where the extracted digests will be added.
     */
    private static void addDigestsForMultiPayments(List<NotificationPaymentInfoInt> payments, List<String> digests) {
        if (!CollectionUtils.isEmpty(payments)) {
            payments.forEach(payment -> {
                if (payment.getPagoPA() != null && payment.getPagoPA().getAttachment() != null) {
                    digests.add(FileUtils.convertBase64toHexUppercase(payment.getPagoPA().getAttachment().getDigests().getSha256()));
                }
                if (payment.getF24() != null && payment.getF24().getMetadataAttachment() != null) {
                    digests.add(FileUtils.convertBase64toHexUppercase(payment.getF24().getMetadataAttachment().getDigests().getSha256()));
                }
            });
        }
    }
}