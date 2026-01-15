package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkUpdateDetails;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkError;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.NotificationReworkErrorCause;
import it.pagopa.pn.deliverypushworkflow.dto.notificationrework.SequenceItemInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.exceptions.NotificationReworkValidationException;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationStatus;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventAction;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventPool;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReworkUpdateHandlerTest {

    @Mock
    private TimelineService timelineService;
    @Mock
    private ReworkRequestEventPool reworkRequestEventPool;

    @InjectMocks
    private ReworkUpdateHandler handler;

    private Action buildAction() {
        NotificationReworkUpdateDetails details = new NotificationReworkUpdateDetails();
        details.setReworkId("reworkId");
        details.setReworkAttempt("ATTEMPT_0");
        details.setReworkRecIndex("RECINDEX_0");
        details.setReworkExpectedStatusCodes(List.of(new SequenceItemInternal()));
        details.setReworkExpectedDeliveryFailureCause("FAILURE_CAUSE");
        details.setReworkExpectedFinalStatus("OK");
        return Action.builder()
                .iun("IUN_1")
                .recipientIndex(0)
                .details(details)
                .build();
    }

    @Test
    void handleNotificationReworkUpdate_success() {
        Action action = buildAction();
        when(timelineService.getTimeline(any(), anyBoolean())).thenReturn(Set.of());

        handler.handleNotificationReworkUpdate(action).block();

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        assertEquals("OK", captor.getValue().getUpdateValidationStatus());
        assertEquals("UPDATE_REQUEST", captor.getValue().getOperation());
        assertEquals("IUN_1", captor.getValue().getIun());
    }

    @Test
    void handleNotificationExpectedFinalStatusCode_INVALID_EXPECTED_STATUS_CODE() {
        NotificationReworkUpdateDetails detail = new NotificationReworkUpdateDetails();
        detail.setReworkAttempt("ATTEMPT_0");
        detail.setReworkRecIndex("RECINDEX_0");
        detail.setReworkExpectedFinalStatus("KO");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();


        Set<TimelineElementInternal> timeline = new HashSet<>();
        TimelineElementInternal timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE);
        timelineElement.setElementId("PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
        timelineElement.setElementId("SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.REFINEMENT);
        timelineElement.setElementId("REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE);
        timelineElement.setElementId("SEND_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
        timelineElement.setElementId("SEND_ANALOG_FEEDBACK.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1");
        timeline.add(timelineElement);

        timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.REFINEMENT);
        timelineElement.setElementId("REFINEMENT.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_1");
        timeline.add(timelineElement);


        NotificationHistoryResponse notificationHistoryResponse = new NotificationHistoryResponse();
        notificationHistoryResponse.setNotificationStatus(NotificationStatus.EFFECTIVE_DATE);

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(timeline);

        handler.handleNotificationReworkUpdate(action).block();

        ArgumentCaptor<ReworkRequestEventAction> captor = ArgumentCaptor.forClass(ReworkRequestEventAction.class);
        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(captor.capture(), any());
        List<NotificationReworkError> capturedErrorList = captor.getValue().getError();
        Assertions.assertEquals(NotificationReworkErrorCause.INVALID_EXPECTED_STATUS_CODE.getCause(), capturedErrorList.getFirst().getCause());
        Assertions.assertEquals("Non è possibile correggere l'ATTEMPT_0 di una notifica con un KO se l'ATTEMPT_1 è già presente", capturedErrorList.getFirst().getDescription());
    }
}