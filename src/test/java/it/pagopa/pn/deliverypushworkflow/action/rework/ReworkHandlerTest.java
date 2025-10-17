package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkValidationDetails;
import it.pagopa.pn.deliverypushworkflow.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.actionmanager.api.ActionApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.api.CheckAddressApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.paperchannel.model.CheckAddressResponse;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventPool;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class ReworkHandlerTest {

    @Mock
    private CheckAddressApi checkAddressApi;
    @Mock
    private ActionApi actionManagerApi;
    @Mock
    private TimelineService timelineService;
    @Mock
    private ReworkRequestEventPool reworkRequestEventPool;
    @Mock
    private PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;
    @Mock
    private TimelineUtils timelineUtils;

    private ReworkHandler reworkHandler;

    @BeforeEach
    void setup() {
        reworkHandler = new ReworkHandler(checkAddressApi, actionManagerApi, timelineService, reworkRequestEventPool, pnDeliveryPushWorkflowConfigs, timelineUtils);
    }

    @Test
    void handleReworkAddressFoundAndValid() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("0");
        detail.setReworkpcRetry("PCRETRY_0");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        TimelineElementInternal timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE);
        timelineElement.setElementId("PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(Set.of(timelineElement));
        when(pnDeliveryPushWorkflowConfigs.getReworkTTLAddressRange()).thenReturn(10);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);

        CheckAddressResponse response = new CheckAddressResponse();
        response.setFound(true);
        response.setEndValidity(Instant.now().plus(20, java.time.temporal.ChronoUnit.DAYS));
        when(checkAddressApi.checkAddress(anyString())).thenReturn(response);

        reworkHandler.handleRework(action);

        verify(actionManagerApi, times(1)).insertAction(any());
        verify(reworkRequestEventPool, never()).scheduleFutureAction(any(), any());
    }

    @Test
    void handleReworkAddressFoundButExpiring() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("0");
        detail.setReworkpcRetry("PCRETRY_0");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        TimelineElementInternal timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE);
        timelineElement.setElementId("PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(Set.of(timelineElement));
        when(pnDeliveryPushWorkflowConfigs.getReworkTTLAddressRange()).thenReturn(1);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);

        CheckAddressResponse response = new CheckAddressResponse();
        response.setFound(true);
        response.setEndValidity(Instant.now().plusSeconds(1));
        when(checkAddressApi.checkAddress(anyString())).thenReturn(response);

        reworkHandler.handleRework(action);

        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(any(), any());
        verify(actionManagerApi, never()).insertAction(any());
    }

    @Test
    void handleReworkAddressNotFound() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("0");
        detail.setReworkpcRetry("PCRETRY_0");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        TimelineElementInternal timelineElement = new TimelineElementInternal();
        timelineElement.setCategory(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE);
        timelineElement.setElementId("PREPARE_ANALOG_DOMICILE.IUN_XLJE-VRQM-VKNQ-202507-K-1.RECINDEX_0.ATTEMPT_0");

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(Set.of(timelineElement));
        when(pnDeliveryPushWorkflowConfigs.getReworkTTLAddressRange()).thenReturn(10);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);

        CheckAddressResponse response = new CheckAddressResponse();
        response.setFound(false);
        when(checkAddressApi.checkAddress(anyString())).thenReturn(response);

        reworkHandler.handleRework(action);

        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(any(), any());
        verify(actionManagerApi, never()).insertAction(any());
    }

    @Test
    void handleReworkTimelineNotFound() {
        NotificationReworkValidationDetails detail = new NotificationReworkValidationDetails();
        detail.setReworkAttempt("0");
        detail.setReworkpcRetry("PCRETRY_0");
        Action action = Action.builder()
                .iun("XLJE-VRQM-VKNQ-202507-K-1")
                .details(detail)
                .recipientIndex(1)
                .build();

        when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(new HashSet<>());
        when(pnDeliveryPushWorkflowConfigs.getReworkTTLAddressRange()).thenReturn(10);
        when(timelineUtils.checkIsNotificationCancellationRequested(any())).thenReturn(false);

        CheckAddressResponse response = new CheckAddressResponse();
        response.setFound(false);
        when(checkAddressApi.checkAddress(anyString())).thenReturn(response);

        reworkHandler.handleRework(action);

        verify(reworkRequestEventPool, times(1)).scheduleFutureAction(any(), any());
        verify(actionManagerApi, never()).insertAction(any());
    }
}