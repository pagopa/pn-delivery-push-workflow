package it.pagopa.pn.deliverypushworkflow.action.analogworkflow;

import it.pagopa.pn.deliverypushworkflow.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.externalchannel.AttachmentDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.paperchannel.SendEventInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AnalogWorkflowUtilsTest {

    private static final String TAX_ID = "tax_id";
    private TimelineUtils timelineUtils;
    private TimelineService timelineService;
    private NotificationUtils notificationUtils;
    private AnalogWorkflowUtils analogWorkflowUtils;

    @BeforeEach
    void setup() {
        timelineService = Mockito.mock(TimelineService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
        analogWorkflowUtils = new AnalogWorkflowUtils(timelineService, timelineUtils, notificationUtils);
    }

    @Test
    void addFailureAnalogFeedbackToTimeline() {
        NotificationInt notificationInt = newNotification();

        List<AttachmentDetailsInt> attachments = new ArrayList<>();
        attachments.add(AttachmentDetailsInt.builder().url("key").build());

        SendAnalogDetailsInt sendPaperDetails = SendAnalogDetailsInt.builder()
                .physicalAddress(
                        PhysicalAddressInt.builder()
                                .province("province")
                                .municipality("munic")
                                .at("at")
                                .build()
                )
                .recIndex(0)
                .sentAttemptMade(0)
                .build();

        SendEventInt sendEventInt = SendEventInt.builder()
                .statusDateTime(Instant.now())
                .statusCode("KO")
                .statusDetail("ABCD")
                .deliveryFailureCause("M1")
                .build();

        final String sendRequestId = "send_request_id";

        when(timelineUtils.buildFailureAnalogFeedbackTimelineElement(
                any(), Mockito.anyInt(), any(), any(), any(), any())).thenReturn(Mockito.mock(TimelineElementInternal.class));
        when(timelineService.addTimelineElement(any(), any()))
                .thenReturn(new it.pagopa.pn.deliverypushworkflow.dto.timeline.AddTimelineElementResponse("timelineElementId", false));

        analogWorkflowUtils.addFailureAnalogFeedbackToTimeline(notificationInt, 1, attachments, sendPaperDetails,sendEventInt, sendRequestId);
        Mockito.verify(timelineUtils).buildFailureAnalogFeedbackTimelineElement(notificationInt, 1, attachments, sendPaperDetails, sendEventInt, sendRequestId);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getPhysicalAddress() {

        NotificationInt notificationInt = newNotification();
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().province("province").municipality("munic").at("at").build();
        NotificationRecipientInt notificationRecipientInt = NotificationRecipientInt.builder().physicalAddress(physicalAddressInt).taxId("testIdRecipient").denomination("Nome Cognome/Ragione Sociale").build();

        int recIndex = 0;
        when(notificationUtils.getRecipientFromIndex(notificationInt, recIndex)).thenReturn(notificationRecipientInt);

        PhysicalAddressInt tmp = analogWorkflowUtils.getPhysicalAddress(notificationInt, recIndex);

        Assertions.assertEquals(tmp, physicalAddressInt);
    }

    private NotificationInt newNotification() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId(TAX_ID)
                                .internalId(TAX_ID + "ANON")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }
}