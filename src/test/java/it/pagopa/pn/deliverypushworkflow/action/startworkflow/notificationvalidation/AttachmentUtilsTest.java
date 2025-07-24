package it.pagopa.pn.deliverypushworkflow.action.startworkflow.notificationvalidation;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypushworkflow.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.AarUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypushworkflow.dto.ext.paperchannel.NotificationChannelType;
import it.pagopa.pn.deliverypushworkflow.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypushworkflow.dto.ext.safestorage.UpdateFileMetadataResponseInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.AarCreationRequestDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.GeneratedF24DetailsInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.NotificationFeePolicy;
import it.pagopa.pn.deliverypushworkflow.legalfacts.AarTemplateType;
import it.pagopa.pn.deliverypushworkflow.legalfacts.StaticAarTemplateChooseStrategy;
import it.pagopa.pn.deliverypushworkflow.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypushworkflow.service.SafeStorageService;
import it.pagopa.pn.deliverypushworkflow.utils.PnSendMode;
import it.pagopa.pn.deliverypushworkflow.utils.PnSendModeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.deliverypushworkflow.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;


class AttachmentUtilsTest {

    private AttachmentUtils attachmentUtils;

    private SafeStorageService safeStorageService;

    private NotificationUtils notificationUtils;

    private PnSendModeUtils pnSendModeUtils;

    private AarUtils aarUtils;

    private NotificationProcessCostService notificationProcessCostService;

    private TimelineUtils timelineUtils;

    @BeforeEach
    void init(){
        safeStorageService = Mockito.mock(SafeStorageService.class);
        pnSendModeUtils = Mockito.mock(PnSendModeUtils.class);
        notificationProcessCostService = Mockito.mock(NotificationProcessCostService.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
        aarUtils = Mockito.mock(AarUtils.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        attachmentUtils = new AttachmentUtils(safeStorageService, notificationProcessCostService, pnSendModeUtils, aarUtils, notificationUtils, timelineUtils);

    }

    @ParameterizedTest
    @CsvSource(value = {
            "RESOLVE_WITH_TIMELINE, false, http", // formatWithDocTag = false
            "URL, true, http?docTag=AAR", // formatWithDocTag = true
    })
    void retrieveAttachmentsAAR(F24ResolutionMode resolveF24Mode, Boolean formatWithDocTag, String expectedResult) {
        //GIVEN
        NotificationInt notification = TestUtils.getNotificationV2();

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        //WHEN
        List<String> attachments1 = attachmentUtils.retrieveAttachments(notification, 0, SendAttachmentMode.AAR, resolveF24Mode, List.of(), formatWithDocTag);

        // THEN
        Assertions.assertNotNull(attachments1);
        Assertions.assertFalse(attachments1.isEmpty());
        Assertions.assertEquals(1, attachments1.size());
        Assertions.assertEquals(expectedResult,attachments1.getFirst());

    }

    @ParameterizedTest
    @CsvSource(value = {
            "RESOLVE_WITH_TIMELINE, false, http, safestorage://test", // formatWithDocTag = false
            "URL, true, http?docTag=AAR, safestorage://test?docTag=DOCUMENT" // formatWithDocTag = true
    })
    void retrieveAttachmentsAAR_DOCUMENTS(F24ResolutionMode resolveF24Mode, Boolean formatWithDocTag, String expectedAarUrl, String expectedDocumentUrl) {
        //GIVEN
        NotificationInt notification = TestUtils.getNotificationV2WithDocument();

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        //WHEN
        List<String> attachments1 = attachmentUtils.retrieveAttachments(notification, 0, SendAttachmentMode.AAR_DOCUMENTS, resolveF24Mode, List.of(), formatWithDocTag);

        // THEN
        Assertions.assertNotNull(attachments1);
        Assertions.assertFalse(attachments1.isEmpty());
        Assertions.assertEquals(2, attachments1.size());
        Assertions.assertEquals(expectedAarUrl, attachments1.getFirst());
        Assertions.assertEquals(expectedDocumentUrl, attachments1.get(1));
    }

    @Test
    void retrieveSendAttachmentModeANALOG_NOTIFICATION() {
        //GIVEN
        PnSendMode pnSendMode = PnSendMode.builder()
                .startConfigurationTime(Instant.now())
                .digitalSendAttachmentMode(SendAttachmentMode.AAR)
                .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS)
                .analogSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
                .aarTemplateTypeChooseStrategy(new StaticAarTemplateChooseStrategy(AarTemplateType.AAR_NOTIFICATION))
                .build();
        Mockito.when(pnSendModeUtils.getPnSendMode(any())).thenReturn(pnSendMode);
        //WHEN
        SendAttachmentMode sendAttachmentMode = attachmentUtils.retrieveSendAttachmentMode(TestUtils.getNotificationV2WithDocument(), NotificationChannelType.ANALOG_NOTIFICATION);
        //THEN
       Assertions.assertEquals(sendAttachmentMode,pnSendMode.getAnalogSendAttachmentMode());
    }

    @Test
    void retrieveSendAttachmentModeSIMPLE_REGISTERED_LETTER() {
        //GIVEN
        PnSendMode pnSendMode = PnSendMode.builder()
                .startConfigurationTime(Instant.now())
                .digitalSendAttachmentMode(SendAttachmentMode.AAR)
                .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS)
                .analogSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
                .aarTemplateTypeChooseStrategy(new StaticAarTemplateChooseStrategy(AarTemplateType.AAR_NOTIFICATION))
                .build();
        Mockito.when(pnSendModeUtils.getPnSendMode(any())).thenReturn(pnSendMode);
        //WHEN
        SendAttachmentMode sendAttachmentMode = attachmentUtils.retrieveSendAttachmentMode(TestUtils.getNotificationV2WithDocument(), NotificationChannelType.SIMPLE_REGISTERED_LETTER);
        //THEN
        Assertions.assertEquals(sendAttachmentMode,pnSendMode.getSimpleRegisteredLetterSendAttachmentMode());
    }

    @Test
    void retrieveSendAttachmentModeDIGITAL_NOTIFICATION() {
        //GIVEN
        PnSendMode pnSendMode = PnSendMode.builder()
                .startConfigurationTime(Instant.now())
                .digitalSendAttachmentMode(SendAttachmentMode.AAR)
                .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS)
                .analogSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
                .aarTemplateTypeChooseStrategy(new StaticAarTemplateChooseStrategy(AarTemplateType.AAR_NOTIFICATION))
                .build();
        Mockito.when(pnSendModeUtils.getPnSendMode(any())).thenReturn(pnSendMode);
        //WHEN
        SendAttachmentMode sendAttachmentMode = attachmentUtils.retrieveSendAttachmentMode(TestUtils.getNotificationV2WithDocument(), NotificationChannelType.DIGITAL_NOTIFICATION);
        //THEN
        Assertions.assertEquals(sendAttachmentMode,pnSendMode.getDigitalSendAttachmentMode());
    }

    /**
     * - sendAttachmentMode: AAR_DOCUMENTS_PAYMENTS
     * - destinatario: singolo.
     */
    @ParameterizedTest
    @CsvSource(value = {
            "RESOLVE_WITH_TIMELINE, false, http, safestorage://test, safestorage://paymentAttach, f24Attachment, 4", // formatWithDocTag = false
            "URL, true, http?docTag=AAR, safestorage://test?docTag=DOCUMENT, safestorage://paymentAttach?docTag=ATTACHMENT_PAGOPA, f24set://IUN_01/0?cost=2&vat=23&docTag=ATTACHMENT_F24, 4", // formatWithDocTag = true
            "RESOLVE_WITH_REPLACED_LIST, true, http?docTag=AAR, safestorage://test?docTag=DOCUMENT, safestorage://paymentAttach?docTag=ATTACHMENT_PAGOPA, replacedF24AttachmentUrls, 4" // formatWithDocTag = true
    })
    void retrieveAttachmentsAAR_DOCUMENTS_PAYMENTS(F24ResolutionMode resolveF24Mode, Boolean formatWithDocTag, String expectedAarUrl, String expectedDocumentUrl, String expectedPagoPaUrl, String expectedF24Url , int expectedSize) {
        //GIVEN
        NotificationInt notification = TestUtils.getNotificationV2WithF24();

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.lenient().when(notificationProcessCostService.notificationProcessCostF24(any(), anyInt(), any(), any(), any(),any())).thenReturn(Mono.just(2));
        Mockito.when(notificationUtils.getRecipientFromIndex(any(),anyInt())).thenReturn(notification.getRecipients().getFirst());

        GeneratedF24DetailsInt generatedF24DetailsInt = new GeneratedF24DetailsInt(0,List.of("f24Attachment"));
        TimelineElementInternal details = new TimelineElementInternal().toBuilder().details(generatedF24DetailsInt).build();
        Mockito.lenient().when(timelineUtils.getGeneratedF24(any(),eq(0))).thenReturn(Optional.of(details));

        //WHEN
        List<String> attachments = attachmentUtils.retrieveAttachments(notification, 0, SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS, resolveF24Mode, List.of("replacedF24AttachmentUrls"), formatWithDocTag);

        // THEN
        Assertions.assertNotNull(attachments);
        Assertions.assertFalse(attachments.isEmpty());
        Assertions.assertEquals(expectedSize, attachments.size());
        Assertions.assertEquals(expectedAarUrl, attachments.getFirst());
        Assertions.assertEquals(expectedDocumentUrl, attachments.get(1));
        Assertions.assertEquals(expectedPagoPaUrl, attachments.get(2));
        Assertions.assertEquals(expectedF24Url, attachments.get(3));
    }

    /**
     *  - sendAttachmentMode: AAR_DOCUMENTS_PAYMENTS
     *  - destinatario: multiplo
     */
    @ParameterizedTest
    @CsvSource(value = {
            "0, safestorage://PN_NOTIFICATION_ATTACHMENTS-91a87a946c0d4c1ba17cd2a0037665db.pdf, safestorage://key, f24set://iun/0?cost=5&vat=22, 5", // formatWithDocTag = false
            "1, safestorage://PN_NOTIFICATION_ATTACHMENTS-91a87a946c0d4c1ba17cd2a0037665db.pdf, safestorage://PN_NOTIFICATION_ATTACHMENTS-a75e827e953c4917b6d1beaf6df56755.pdf, f24set://iun/1?cost=8&vat=22, 8" // formatWithDocTag = false
    })
    void getAttachmentsAndPaymentsByRecipient(int recIndexRecipient, String expectedDocumentUrl,  String expectedPagoPaUrl, String expectedF24Url, int cost) {
        //GIVEN
        NotificationInt notification = getNotificationWithMultipleRecipients();

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.when(notificationUtils.getRecipientFromIndex(notification, recIndexRecipient)).thenReturn(notification.getRecipients().get(recIndexRecipient));
        Mockito.when(notificationProcessCostService.notificationProcessCostF24(any(), eq(recIndexRecipient), any(), any(), any(),any())).thenReturn(Mono.just(cost));

        //WHEN
        List<String> attachmentsRecipient = attachmentUtils.retrieveAttachments(notification, recIndexRecipient, SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS, F24ResolutionMode.URL, List.of(), false);

        System.out.println(attachmentsRecipient);
        Assertions.assertEquals(expectedDocumentUrl, attachmentsRecipient.get(1));
        Assertions.assertEquals(expectedPagoPaUrl, attachmentsRecipient.get(2));
        Assertions.assertEquals(expectedF24Url, attachmentsRecipient.get(3));
    }

    @Test
    void getAttachmentsAndPaymentsWithEmptyPayments() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotificationV2WithoutPayments();

        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(AarGenerationDetailsInt.builder().generatedAarUrl("http").build());
        Mockito.when(notificationUtils.getRecipientFromIndex(notification, 0)).thenReturn(notification.getRecipients().getFirst());

        //WHEN
        List<String> attachmentsRecipient = attachmentUtils.retrieveAttachments(notification, 0, SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS, F24ResolutionMode.RESOLVE_WITH_TIMELINE, List.of(), false);

        //THEN
        Assertions.assertEquals(2, attachmentsRecipient.size());
    }

    /**
     *  Caso di test in cui replacedF24AttachmentUrls = null
     */
    @Test
    void retrieveAttachmentsAndPaymentsWithNullReplacedF24() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotificationV2WithF24();

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(),anyInt())).thenReturn(notification.getRecipients().getFirst());

        //WHEN
        List<String> attachments = attachmentUtils.retrieveAttachments(notification, 0, SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS, F24ResolutionMode.RESOLVE_WITH_REPLACED_LIST, null, true);

        // THEN
        Assertions.assertEquals(3, attachments.size());
    }

    @Test
    void retrieveAttachmentsAAR_DOCUMENTS_PAYMENTSFail() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotificationV2WithF24();
        List<String> replaced = List.of();

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(),anyInt())).thenReturn(notification.getRecipients().getFirst());
        Mockito.when(timelineUtils.getGeneratedF24(any(),any())).thenReturn(Optional.empty());

        // THEN
        assertThrows(PnInternalException.class, () -> attachmentUtils.retrieveAttachments(notification, 0, SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS, F24ResolutionMode.RESOLVE_WITH_TIMELINE, replaced , false));
    }

    @Test
    void retrieveAttachmentsAAR_DOCUMENTS_PAYMENTSFailException() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotificationV2WithF24();
        List<String> replaced = List.of();

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(),anyInt())).thenReturn(notification.getRecipients().getFirst());
        Mockito.when(timelineUtils.getGeneratedF24(any(),any())).thenThrow(new RuntimeException("aaaa"));

        // THEN
        assertThrows(RuntimeException.class, () -> attachmentUtils.retrieveAttachments(notification, 0, SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS, F24ResolutionMode.RESOLVE_WITH_TIMELINE, replaced , false));
    }

    @Test
    void changeAttachmentsRetention() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        UpdateFileMetadataResponseInt resp = new UpdateFileMetadataResponseInt();
        resp.setResultCode("200.00");

        Mockito.when(safeStorageService.updateFileMetadata(any(), any())).thenReturn(Mono.just(resp));

        //WHEN
        attachmentUtils.changeAttachmentsRetention(notification, 20).blockFirst();

        //THEN
        Mockito.verify(safeStorageService, Mockito.times(2)).updateFileMetadata(any(), any());
    }

    @Test
    void changeAttachmentsRetentionKO() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(safeStorageService.updateFileMetadata(any(), any())).thenThrow(new PnInternalException("test", "test"));

        Flux<Void> flux = attachmentUtils.changeAttachmentsRetention(notification, 20);
        //WHEN
        assertThrows(PnInternalException.class, flux::blockFirst);

        //THEN
        Mockito.verify(safeStorageService, Mockito.times(1)).updateFileMetadata(any(), any());
    }

    @Test
    void getAarWithRadd() {
        var notificationInt = getNotificationInt(getNotificationRecipientInt());
        when(aarUtils.getAarCreationRequestDetailsInt(notificationInt, 0))
                .thenReturn(AarCreationRequestDetailsInt.builder().aarTemplateType(AarTemplateType.AAR_NOTIFICATION_RADD_ALT).build());

        var raddAarTypeActual = attachmentUtils.getAarWithRadd(notificationInt, 0);
        Assertions.assertEquals(true, raddAarTypeActual);
    }

    private NotificationInt getNotificationInt(NotificationRecipientInt recipient) {
        return NotificationTestBuilder.builder()
                .withIun("iun_01")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();
    }

    private NotificationRecipientInt getNotificationRecipientInt() {
        String taxId = "TaxId";
        return NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_" + taxId)
                .withDigitalDomicile(
                        LegalDigitalAddressInt.builder()
                                .address("address")
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .build()
                )
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(PagoPaInt.builder()
                                        .attachment(NotificationDocumentInt.builder()
                                                .ref(NotificationDocumentInt.Ref.builder()
                                                        .key("keyPagoPaForm")
                                                        .build())
                                                .digests(NotificationDocumentInt.Digests.builder()
                                                        .sha256(Base64.getEncoder().encodeToString("keyPagoPaForm".getBytes()))
                                                        .build())
                                                .build())
                                        .build())
                                .build()
                ))
                .build();
    }

    private NotificationInt getNotificationWithMultipleRecipients() {

        NotificationRecipientInt notificationRecipient1 = NotificationRecipientTestBuilder.builder()
                .withRecipientType(RecipientTypeInt.PF)
                .withTaxId("CLMCST42R12D969Z")
                .withInternalId("")
                .withPhysicalAddress(
                        PhysicalAddressInt.builder()
                                .at("Presso")
                                .address("Via@ok_AR")
                                .addressDetails("scala b")
                                .zip("40100")
                                .municipality("Milano")
                                .municipalityDetails("Milano")
                                .province("MI")
                                .foreignState("ITALIA")
                                .build()
                )
                .withPayments(
                        Collections.singletonList(
                                NotificationPaymentInfoInt.builder()
                                        .pagoPA(PagoPaInt.builder()
                                                .noticeCode("302011681384967173")
                                                .creditorTaxId("77777777777")
                                                .attachment(NotificationDocumentInt.builder()
                                                        .digests(NotificationDocumentInt.Digests.builder()
                                                                .sha256("PagoPaSha256").build())
                                                        .ref(NotificationDocumentInt.Ref.builder()
                                                                .key("key")
                                                                .build())
                                                        .build())
                                                .build())
                                        .f24(F24Int.builder()
                                                .applyCost(true)
                                                .metadataAttachment(NotificationDocumentInt.builder()
                                                        .digests(NotificationDocumentInt.Digests.builder()
                                                                .sha256("F24Sha256").build())
                                                        .build())
                                                .title("f24Title")
                                                .build())
                                        .build()
                        )
                )
                .build();

        NotificationRecipientInt notificationRecipient2 = NotificationRecipientTestBuilder.builder()
                .withRecipientType(RecipientTypeInt.PG)
                .withTaxId("MSSLGU51P10A087J")
                .withInternalId("")
                .withDenomination("Cucumber_Society")
                .withPhysicalAddress(
                        PhysicalAddressInt.builder()
                                .at("Presso")
                                .address("Via senza nome")
                                .addressDetails("scala b")
                                .zip("40100")
                                .municipality("Milano")
                                .municipalityDetails("Milano")
                                .province("MI")
                                .foreignState("ITALIA")
                                .build()
                )
                .withDigitalDomicile(
                        LegalDigitalAddressInt.builder()
                                .address("testpagopa2@pnpagopa.postecert.local")
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .build()
                )
                .withPayments(
                        Collections.singletonList(
                                NotificationPaymentInfoInt.builder()
                                        .pagoPA(
                                                PagoPaInt.builder()
                                                        .noticeCode("302011681384967181")
                                                        .creditorTaxId("77777777777")
                                                        .attachment(NotificationDocumentInt.builder()
                                                                .ref(NotificationDocumentInt.Ref.builder()
                                                                        .key("PN_NOTIFICATION_ATTACHMENTS-a75e827e953c4917b6d1beaf6df56755.pdf")
                                                                        .versionToken("v1")
                                                                        .build())
                                                                .digests(NotificationDocumentInt.Digests.builder()
                                                                        .sha256("jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE=")
                                                                        .build())
                                                                .build())
                                                        .build()
                                        )
                                        .f24(F24Int.builder()
                                                .applyCost(true)
                                                .metadataAttachment(NotificationDocumentInt.builder()
                                                        .digests(NotificationDocumentInt.Digests.builder()
                                                                .sha256("F24Sha256").build())
                                                        .build())
                                                .title("f24Title")
                                                .build())
                                        .build()
                        )
                ).build();

        return NotificationInt.builder()
                .iun("iun")
                .paProtocolNumber("302011681384967158")
                .subject("notifica analogica con cucumber")
                .paFee(1)
                .vat(22)
                .physicalCommunicationType(ServiceLevelTypeInt.AR_REGISTERED_LETTER)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .documents(List.of(NotificationDocumentInt.builder()
                        .ref(NotificationDocumentInt.Ref.builder()
                                .key("PN_NOTIFICATION_ATTACHMENTS-91a87a946c0d4c1ba17cd2a0037665db.pdf")
                                .versionToken("v1")
                                .build())
                        .digests( NotificationDocumentInt.Digests.builder()
                                .sha256( "jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE" )
                                .build() )
                        .build()))
                .sender(NotificationSenderInt.builder()
                        .paTaxId("80016350821")
                        .paDenomination("Comune di palermo")
                        .build())
                .recipients(List.of(notificationRecipient1, notificationRecipient2))
                .build();
    }
}
