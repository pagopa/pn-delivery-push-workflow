package it.pagopa.pn.deliverypushworkflow.legalfacts;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.qr.QrUrlCodecService;
import it.pagopa.pn.commons.utils.qr.models.UrlData;
import it.pagopa.pn.deliverypushworkflow.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.legalfacts.AARInfo;
import it.pagopa.pn.deliverypushworkflow.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.templatesengine.model.*;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.templatesengine.TemplatesClient;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.templatesengine.TemplatesClientPec;
import it.pagopa.pn.deliverypushworkflow.utils.PnSendMode;
import it.pagopa.pn.deliverypushworkflow.utils.PnSendModeUtils;
import it.pagopa.pn.deliverypushworkflow.utils.QrCodeUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_CONFIGURATION_NOT_FOUND;
import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALID_TEMPLATE;
import static it.pagopa.pn.deliverypushworkflow.service.mapper.TemplatesEngineMapper.*;


@Slf4j
@AllArgsConstructor
@Component
public class LegalFactGeneratorTemplates implements LegalFactGenerator {

    private final CustomInstantWriter instantWriter;
    private final PhysicalAddressWriter physicalAddressWriter;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;
    private final PnSendModeUtils pnSendModeUtils;
    private final TemplatesClient templatesClient;
    private final TemplatesClientPec templatesClientPec;
    private final QrUrlCodecService qrUrlCodecService;

    /**
     * Generates the legal fact for the viewing of a notification.
     *
     * @param iun           the unique identifier of the notification (IUN).
     * @param recipient     the recipient of the notification, represented by a
     *                      {@link NotificationRecipientInt} object containing information such
     *                      as name (denomination) and tax ID.
     * @param delegateInfo  the delegate's information (if present), represented by a
     *                      {@link DelegateInfoInt} object containing name and tax ID.
     * @param timeStamp     the timestamp of when the notification was viewed, as an {@link Instant} object.
     * @param notification  the {@link NotificationInt} object representing the full notification,
     *                      from which additional information such as additional languages is extracted.
     * @return a byte array representing the pdf legal fact of the notification viewing.
     * @throws IllegalArgumentException if any required parameter is null or contains incomplete data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationViewedLegalFact} object and return the expected byte array.
     */
    @Override
    public byte[] generateNotificationViewedLegalFact(String iun,
                                                      NotificationRecipientInt recipient,
                                                      DelegateInfoInt delegateInfo,
                                                      Instant timeStamp,
                                                      NotificationInt notification) {
        log.info("retrieve NotificationViewedLegalFact template for iun {}", iun);
        NotificationViewedLegalFact notificationViewedLegalFact =
                notificationViewedLegalFact(
                        iun,
                        recipient,
                        delegateInfo,
                        timeStamp,
                        instantWriter);
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationViewedLegalFact(language, notificationViewedLegalFact);
    }

    /**
     * Generates the legal fact for the PEC delivery workflow.
     *
     * @param feedbackFromExtChannelList the list of {@link SendDigitalFeedbackDetailsInt} objects
     *                                   representing feedback from external digital channels, including
     *                                   delivery status and notification dates.
     * @param notification               the {@link NotificationInt} object containing details of the
     *                                   notification, such as its unique identifier (IUN).
     * @param recipient                  the recipient of the notification, represented by a
     *                                   {@link NotificationRecipientInt} object, containing information
     *                                   such as name (denomination) and tax ID.
     * @param status                     the {@link EndWorkflowStatus} representing the final status
     *                                   of the PEC delivery workflow (e.g., completed, failed).
     * @param completionWorkflowDate     the {@link Instant} representing the timestamp when the
     *                                   PEC delivery workflow was completed.
     * @return a byte array representing the pdf PEC delivery workflow legal fact.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is correctly configured to handle the generated
     * {@link PecDeliveryWorkflowLegalFact} object and return the expected byte array.
     */
    @Override
    public byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList,
                                                       NotificationInt notification,
                                                       NotificationRecipientInt recipient,
                                                       EndWorkflowStatus status,
                                                       Instant completionWorkflowDate) {
        log.info("retrieve PecDeliveryWorkflowLegalFact template for iun {}", notification.getIun());
        PecDeliveryWorkflowLegalFact pecDeliveryWorkflowLegalFact =
                pecDeliveryWorkflowLegalFact(
                        feedbackFromExtChannelList,
                        notification,
                        recipient,
                        status,
                        completionWorkflowDate,
                        instantWriter);
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.pecDeliveryWorkflowLegalFact(language, pecDeliveryWorkflowLegalFact);
    }

    /**
     * Generates the legal fact for the failure of the analog delivery workflow.
     *
     * @param notification       the {@link NotificationInt} object containing the details of the notification,
     *                           such as its unique identifier (IUN).
     * @param recipient          the recipient of the notification, represented by a
     *                           {@link NotificationRecipientInt} object, which includes information such
     *                           as name (denomination) and tax ID.
     * @param status             the {@link EndWorkflowStatus} representing the final status of the analog
     *                           delivery workflow (e.g., failed).
     * @param failureWorkflowDate the {@link Instant} representing the timestamp when the analog delivery
     *                            workflow failure occurred.
     * @return a byte array representing the pdf legal fact for the analog delivery workflow failure.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link AnalogDeliveryWorkflowFailureLegalFact} object and return the expected byte array.
     */
    @Override
    public byte[] generateAnalogDeliveryFailureWorkflowLegalFact(NotificationInt notification,
                                                                 NotificationRecipientInt recipient,
                                                                 EndWorkflowStatus status,
                                                                 Instant failureWorkflowDate) {
        log.info("retrieve AnalogDeliveryFailureWorkflowLegalFact template for iun {}", notification.getIun());
        AnalogDeliveryWorkflowFailureLegalFact analogDeliveryWorkflowFailureLegalFact =
                analogDeliveryWorkflowFailureLegalFact(
                        notification,
                        recipient,
                        failureWorkflowDate,
                        instantWriter);
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.analogDeliveryWorkflowFailureLegalFact(language, analogDeliveryWorkflowFailureLegalFact);
    }

    /**
     * Generates the legal fact for a cancelled notification.
     *
     * @param notification                      the {@link NotificationInt} object containing details
     *                                          about the notification, including its unique identifier (IUN),
     *                                          sender information, and recipients.
     * @param notificationCancellationRequestDate the {@link Instant} representing the timestamp when
     *                                            the notification cancellation request was made.
     * @return a byte array representing the legal fact for the cancelled notification.
     *
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationCancelledLegalFact} object and return the expected pdf byte array.
     */
    @Override
    public byte[] generateNotificationCancelledLegalFact(NotificationInt notification, Instant notificationCancellationRequestDate) {
        log.info("retrieve NotificationCancelledLegalFact template for iun {}", notification.getIun());
        NotificationCancelledLegalFact cancelledLegalFact =
                cancelledLegalFact(
                        notification,
                        notificationCancellationRequestDate,
                        instantWriter);
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationCancelledLegalFact(language, cancelledLegalFact);
    }

    /**
     * Generates the AAR subject for a notification.
     *
     * @param notification the {@link NotificationInt} object containing details about the notification,
     *                     including its unique identifier (IUN) and sender information.
     * @return a {@link String} representing the subject line for the AAR of the notification.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationAarForSubject} object and return the expected subject string.
     */
    @Override
    public String generateNotificationAARSubject(NotificationInt notification) {
        log.info("retrieve NotificationAARSubject template for iun {}", notification.getIun());
        NotificationAarForSubject notificationAARSubject = notificationAARSubject(notification);
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationAarForSubject(language, notificationAARSubject);
    }

    /**
     * Generates the AAR subject for a notification.
     *
     * @param notification the {@link NotificationInt} object containing details
     *                     about the notification, including its unique identifier (IUN)
     *                     and sender information.
     * @return a {@link AARInfo} representing the AAR info for the notification.
     *
     * @throws IllegalArgumentException if the notification is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationAarForSubject} object and return the expected subject string.
     */
    @Override
    public AARInfo generateNotificationAAR(NotificationInt notification, NotificationRecipientInt recipient, String quickAccessToken) {
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        PnSendMode pnSendMode = pnSendModeUtils.getPnSendMode(notification.getSentAt());
        String qrCodeQuickAccessUrlAarDetail = this.getQrCodeQuickAccessUrlAarDetail(recipient, quickAccessToken);
        String accessUrl = this.getAccessUrl(recipient);
        String accessUrlLabel = this.getAccessUrlLabel(recipient);
        String perfezionamentoLink = this.getPerfezionamentoLink();
        String perfezionamentoLinkLabel = this.getPerfezionamentoLinkLabel();
        String accessLink = this.getAccessLink();
        if (pnSendMode != null) {
            final AarTemplateChooseStrategy aarTemplateTypeChooseStrategy = pnSendMode.getAarTemplateTypeChooseStrategy();
            final AarTemplateType aarTemplateType = aarTemplateTypeChooseStrategy.choose(recipient.getPhysicalAddress());
            log.debug("aarTemplateType generated is ={} - iun={}", aarTemplateType, notification.getIun());
            byte[] bytesArrayGeneratedAar = new byte[0];
            switch (aarTemplateType) {
                case AAR_NOTIFICATION -> {
                    log.info("retrieve NotificationAAR template for iun {}", notification.getIun());
                    NotificationAar notificationAAR =
                            notificationAAR(
                                    notification,
                                    recipient,
                                    qrCodeQuickAccessUrlAarDetail,
                                    accessUrl,
                                    accessUrlLabel,
                                    perfezionamentoLink,
                                    perfezionamentoLinkLabel,
                                    this.buildAarSenderLogo(notification.getSender().getPaId()));
                    bytesArrayGeneratedAar = templatesClient.notificationAar(language, notificationAAR);
                }
                case AAR_NOTIFICATION_RADD_ALT -> {
                    log.info("retrieve NotificationAARRADDalt template for iun {}", notification.getIun());
                    NotificationAarRaddAlt notificationAARRADDalt =
                            notificationAARRADDalt(
                                    notification,
                                    recipient,
                                    qrCodeQuickAccessUrlAarDetail,
                                    accessUrl,
                                    accessUrlLabel,
                                    accessLink,
                                    this.getAccessLinkLabel(),
                                    perfezionamentoLink,
                                    perfezionamentoLinkLabel,
                                    pnDeliveryPushWorkflowConfigs.getWebapp().getRaddPhoneNumber(),
                                    this.buildAarSenderLogo(notification.getSender().getPaId()));
                    bytesArrayGeneratedAar = templatesClient.notificationAarRaddAlt(language, notificationAARRADDalt);
                }
                case AAR_NOTIFICATION_RADD -> throw new PnInternalException("NotificationAAR_RADD not implemented", ERROR_CODE_DELIVERYPUSH_INVALID_TEMPLATE);
            }
            return AARInfo.builder()
                    .bytesArrayGeneratedAar(bytesArrayGeneratedAar)
                    .templateType(aarTemplateType)
                    .build();
        } else {
            String msg = String.format("There isn't correct AAR configuration for date=%s - iun=%s", notification.getSentAt(), notification.getIun());
            log.error(msg);
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_CONFIGURATION_NOT_FOUND);
        }
    }

    /**
     * Generates the AAR body for an analog notification.
     *
     * @param notification      the {@link NotificationInt} object containing details about the notification,
     *                          including its unique identifier (IUN).
     * @param recipient         the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                          including relevant details such as contact information.
     * @param quickAccess  a {@link String} representing the value used to generate the quick access URL
     *                          for the notification details.
     * @return a {@link String} representing the body of the AAR email for the notification.
     *
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationAarForEmailAnalog} object and return the expected email body string.
     */
    @Override
    public String generateNotificationAARBodyForEmailAnalog(NotificationInt notification, NotificationRecipientInt recipient, String quickAccess) {
        log.info("retrieve NotificationAARBody template for iun {}", notification.getIun());
        NotificationAarForEmailAnalog notificationAAR =
                notificationAarForEmailAnalog(
                        notification,
                        this.getPerfezionamentoLink(),
                        this.getQuickAccessLink(recipient, quickAccess),
                        this.getFAQSendURL(),
                        this.getAccessUrl(recipient));
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationAarForEmailAnalog(language, notificationAAR);
    }

    /**
     * Generates the AAR body for a digital notification.
     *
     * @param notification      the {@link NotificationInt} object containing details about the notification,
     *                          including its unique identifier (IUN).
     * @param recipient         the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                          including relevant details such as contact information.
     * @param quickAccess  a {@link String} representing the value used to generate the quick access URL
     *                          for the notification details.
     * @return a {@link String} representing the body of the AAR email for the notification.
     *
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationAarForEmailDigital} object and return the expected email body string.
     */
    @Override
    public String generateNotificationAARBodyForEmailDigital(NotificationInt notification, NotificationRecipientInt recipient, String quickAccess) {
        log.info("retrieve NotificationAARBody template for iun {}", notification.getIun());
        NotificationAarForEmailDigital notificationAAR =
                notificationAarForEmailDigital(
                        notification,
                        recipient,
                        this.getPerfezionamentoLink(),
                        this.getQuickAccessLink(recipient, quickAccess),
                        this.getFAQSendURL(),
                        this.getAccessUrl(recipient));
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationAarForEmailDigital(language, notificationAAR);
    }

    /**
     * Generates the AAR body for a PEC notification.
     *
     * @param notification      the {@link NotificationInt} object containing details about the notification,
     *                          including its unique identifier (IUN) and sender information.
     * @param recipient         the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                          including their tax ID and other relevant details.
     * @param quickAccessToken  a {@link String} representing the token used to generate the quick access URL
     *                          for the notification details.
     * @return a {@link String} representing the PEC email body for the AAR of the notification.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationAarForPec} object and return the expected PEC email body string.
     */
    @Override
    public String generateNotificationAARPECBody(NotificationInt notification,
                                                 NotificationRecipientInt recipient,
                                                 String quickAccessToken) {
        log.info("retrieve NotificationAARPECBody template for iun {}", notification.getIun());
        NotificationAarForPec notificationAAR = notificationAarForPec(
                notification,
                recipient,
                this.getQuickAccessLink(recipient, quickAccessToken),
                this.getPerfezionamentoLink(),
                this.getFAQSendURL(),
                this.getAccessUrl(recipient),
                recipient.getRecipientType().getValue());
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClientPec.parametrizedNotificationAarForPec(language, notificationAAR);
    }

    /**
     * Generates the AAR for an SMS analog notification.
     *
     * @param notification the {@link NotificationInt} object containing details about the notification,
     *                     including its unique identifier (IUN) and sender information.
     * @return a {@link String} representing the SMS body for the AAR of the notification.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationAarForSmsAnalog} object and return the expected SMS body string.
     */
    @Override
    public String generateNotificationAARForSMSAnalog(NotificationInt notification) {
        log.info("retrieve NotificationAARForSMS template for iun {}", notification.getIun());
        NotificationAarForSmsAnalog notificationAARForSMS = notificationAarForSmsAnalog(notification);
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationAarForSmsAnalog(language, notificationAARForSMS);
    }

    /**
     * Generates the AAR for an SMS digital notification.
     *
     * @param notification the {@link NotificationInt} object containing details about the notification,
     *                     including its unique identifier (IUN) and sender information.
     * @return a {@link String} representing the SMS body for the AAR of the notification.
     * @throws IllegalArgumentException if any required parameter is null or contains invalid data.
     *
     * <p><strong>Note:</strong></p>
     * Ensure that {@code templatesClient} is properly configured to handle the generated
     * {@link NotificationAarForSmsDigital} object and return the expected SMS body string.
     */
    @Override
    public String generateNotificationAARForSMSDigital(NotificationInt notification) {
        log.info("retrieve NotificationAARForSMS template for iun {}", notification.getIun());
        NotificationAarForSmsDigital notificationAARForSMS = notificationAarForSmsDigital(notification);
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.notificationAarForSmsDigital(language, notificationAARForSMS);
    }

    /**
     * Retrieves the label for the access URL associated with a notification recipient.
     *
     * @param recipient the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                  used to retrieve the access URL.
     * @return a {@link String} representing the label of the access URL, typically the host without the "www." prefix.
     *         If the URL is invalid, returns the full access URL.
     */
    private String getAccessUrlLabel(NotificationRecipientInt recipient) {
        try {
            String host = URI.create(getAccessUrl(recipient)).toURL().getHost();
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (MalformedURLException e) {
            log.warn("cannot get host", e);
            return getAccessUrl(recipient);
        }
    }

    /**
     * Generates a Base64-encoded QR code image for quick access, using the provided recipient and access token.
     *
     * @param recipient the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                  used to generate the quick access URL.
     * @param quickAccessToken the token used to generate the quick access URL, ensuring secure access to the resource.
     * @return a {@link String} representing the Base64-encoded QR code image in a data URI format.
     */
    private String getQrCodeQuickAccessUrlAarDetail(NotificationRecipientInt recipient, String quickAccessToken) {
        String url = getQuickAccessLink(recipient, quickAccessToken);
        // Definire altezza e larghezza del qrcode
        return "data:image/png;base64, " .concat(Base64.getEncoder().encodeToString(QrCodeUtils.generateQRCodeImage(url, 180, 180,
                pnDeliveryPushWorkflowConfigs.getErrorCorrectionLevelQrCode())));
    }

    /**
     * Generates a quick access link URL for a given recipient, appending the quick access token to the base URL.
     *
     * @param recipient the {@link NotificationRecipientInt} object representing the recipient of the notification,
     *                  used to retrieve the base access URL.
     * @param quickAccess the value used to generate the quick access link, typically used for secure access.
     * @return a {@link String} representing the full quick access URL, including the token as a query parameter.
     */
    private String getQuickAccessLink(NotificationRecipientInt recipient, String quickAccess) {
        UrlData urlData = new UrlData();
        urlData.setRecipientType(it.pagopa.pn.commons.utils.qr.models.RecipientTypeInt.valueOf(recipient.getRecipientType().name()));
        log.debug("getQrCodeQuickAccessUrlAarDetail: {}", quickAccess);
        return qrUrlCodecService.encode(quickAccess, urlData);
    }

    /**
     * Generates a link to the "perfezionamento" page of the web application.
     *
     * @return a {@link String} representing the complete URL for the "perfezionamento" page.
     */
    private String getPerfezionamentoLink() {
        return pnDeliveryPushWorkflowConfigs.getWebapp().getLandingUrl() + "perfezionamento";
    }

    /**
     * Generates the label for the "perfezionamento" link by appending the "perfezionamento" path
     * to the base access link label.
     *
     * @return a {@link String} representing the complete label for the "perfezionamento" link.
     */
    private String getPerfezionamentoLinkLabel() {
        return this.getAccessLinkLabel() + "/perfezionamento";
    }

    private String getAccessLink() {
        return pnDeliveryPushWorkflowConfigs.getWebapp().getLandingUrl();
    }

    /**
     * Retrieves the host name from the base landing URL configured in the application settings,
     * and removes the "www." prefix if it exists.
     *
     * @return a {@link String} representing the host name from the landing URL, with "www." removed if present.
     */
    private String getAccessLinkLabel() {
        try {
            String host = URI.create(pnDeliveryPushWorkflowConfigs.getWebapp().getLandingUrl()).toURL().getHost();
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (MalformedURLException e) {
            log.warn("cannot get host", e);
            return pnDeliveryPushWorkflowConfigs.getWebapp().getLandingUrl();
        }
    }

    private String getFAQAccessLink() {
        return pnDeliveryPushWorkflowConfigs.getWebapp().getLandingUrl() + pnDeliveryPushWorkflowConfigs.getWebapp().getFaqUrlTemplateSuffix();
    }

    private String getFAQSendURL() {
        return this.getFAQAccessLink() + "#" + pnDeliveryPushWorkflowConfigs.getWebapp().getFaqSendHash();
    }

    /**
     * Returns the appropriate access URL for a recipient based on their type.
     *
     * @param recipient the recipient object containing information about the recipient's type.
     * @return a {@link String} representing the access URL based on the recipient's type.
     */
    private String getAccessUrl(NotificationRecipientInt recipient) {
        return RecipientTypeInt.PF == recipient.getRecipientType()
                ? pnDeliveryPushWorkflowConfigs.getWebapp().getDirectAccessUrlTemplatePhysical()
                : pnDeliveryPushWorkflowConfigs.getWebapp().getDirectAccessUrlTemplateLegal();
    }

    /**
     * Determines the language to be used for the notification based on the provided list of additional languages.
     *
     * @param additionalLanguages a {@link List} of {@link String} representing the additional languages to be considered.
     *                            If the list is empty or null, the default language (Italian) is returned.
     * @return a {@link LanguageEnum} representing the selected language. It returns {@link LanguageEnum#IT}
     *         if no additional languages are available or enabled, otherwise the first language from the list.
     * @throws IllegalArgumentException if the provided list contains invalid language values.
     */
    private LanguageEnum getLanguage(List<String> additionalLanguages) {
        return (!pnDeliveryPushWorkflowConfigs.isAdditionalLangsEnabled() || CollectionUtils.isEmpty(additionalLanguages))
                ? LanguageEnum.IT : LanguageEnum.fromValue(additionalLanguages.getFirst());
    }

    /**
     * Builds the URL for the AAR sender logo by replacing the placeholder in the template with the given PA ID.
     *
     * @param paId the PA ID to be inserted into the URL template
     * @return the formatted URL containing the specified PA ID
     */
    private String buildAarSenderLogo(String paId) {
        String aarUrlTemplate = pnDeliveryPushWorkflowConfigs.getWebapp().getAarSenderLogoUrlTemplate();
        return aarUrlTemplate.replace("<PA_ID>", paId);
    }

    @Override
    public byte[] generateAnalogDeliveryWorkflowTimeoutLegalFact(NotificationInt notification,
                                                                 NotificationRecipientInt recipient,
                                                                 PhysicalAddressInt physicalAddress,
                                                                 String sentAttemptMade,
                                                                 Instant timeoutDate) {
        log.info("retrieve AnalogDeliveryWorkflowTimeoutLegalFact template for iun {}", notification.getIun());
        AnalogDeliveryWorkflowTimeoutLegalFact analogDeliveryWorkflowTimeoutLegalFact =
                analogDeliveryWorkflowTimeoutLegalFact(
                        notification.getIun(),
                        timeoutDate,
                        instantWriter,
                        recipient,
                        sentAttemptMade,
                        physicalAddress,
                        physicalAddressWriter);
        LanguageEnum language = getLanguage(notification.getAdditionalLanguages());
        return templatesClient.analogDeliveryWorkflowTimeoutLegalFact(language, analogDeliveryWorkflowTimeoutLegalFact);
    }


}
