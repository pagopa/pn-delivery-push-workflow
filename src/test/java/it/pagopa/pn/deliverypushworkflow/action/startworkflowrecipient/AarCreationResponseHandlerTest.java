package it.pagopa.pn.deliverypushworkflow.action.startworkflowrecipient;

import it.pagopa.pn.deliverypushworkflow.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypushworkflow.action.it.CommonTestConfiguration;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.AddTimelineElementResponse;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.AarCreationRequestDetailsInt;
import it.pagopa.pn.deliverypushworkflow.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

class AarCreationResponseHandlerTest extends CommonTestConfiguration {

    @MockitoBean
    NotificationService notificationService;
    @MockitoBean
    TimelineService timelineService;
    @MockitoBean
    @SuppressWarnings("unused")
    TimelineUtils timelineUtils;
    @Autowired
    AarCreationResponseHandler handler;

    @Test
    void testHandleAarCreationResponse(){

        ConsoleAppenderCustom.initializeLog();
        Instant notificationSentAt = Instant.now();

        String iun="HETX-DAGU-VJWG-202306-Y-1";
        String timelineId="AAR_CREATION_REQUEST.IUN_HETX-DAGU-VJWG-202306-Y-1.RECINDEX_0";
        DocumentCreationTypeInt docType = DocumentCreationTypeInt.AAR;

        Mockito.when(timelineService.addTimelineElement(Mockito.any(), Mockito.any()))
            .thenReturn(new AddTimelineElementResponse(null, true));

        AarCreationRequestDetailsInt aarCreationRequestDetailsInt = AarCreationRequestDetailsInt.builder()
            .recIndex(0)
            .aarKey("aarKey")
            .numberOfPages(1)
            .build();
        Optional<AarCreationRequestDetailsInt> value = Optional.of(aarCreationRequestDetailsInt);
        Mockito.when(timelineService.getTimelineElementDetails(iun, timelineId, AarCreationRequestDetailsInt.class))
            .thenReturn(value);
        DocumentCreationResponseActionDetails actionDetails = DocumentCreationResponseActionDetails.builder()
            .key("key").timelineId(timelineId).documentCreationType(docType)
            .build();

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(buildNotificationInt(notificationSentAt));
        handler.handleAarCreationResponse(iun,0, actionDetails);

        //Then
        ConsoleAppenderCustom.checkWarningLogs("[{}] {} - File already present saving AAR fileKey={} iun={} recIndex={}");
    }

    private NotificationInt buildNotificationInt(Instant sentAt) {
        return NotificationInt.builder()
                .iun("IUN")
                .paProtocolNumber("PA123")
                .subject("SUBJECT")
                .paFee(1)
                .sentAt(sentAt)
                .sender(NotificationSenderInt.builder()
                        .paId("pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .build()
                ))
                .build();
    }

}
