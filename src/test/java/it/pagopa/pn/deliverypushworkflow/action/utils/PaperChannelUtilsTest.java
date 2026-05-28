package it.pagopa.pn.deliverypushworkflow.action.utils;

import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class PaperChannelUtilsTest {

    @Mock
    private TimelineService timelineService;

    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;

    private PaperChannelUtils paperChannelUtils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        paperChannelUtils = new PaperChannelUtils(timelineService, timelineUtils, pnDeliveryPushWorkflowConfigs);
    }

    @Test
    void addPrepareAnalogFailureTimelineElement_shouldPassNull_whenFoundAddressIsNull() {
        String prepareRequestId = "REQ-1";
        String failureCause = "CAUSE-1";
        Integer recIndex = 1;
        NotificationInt notification = NotificationInt.builder().iun("IUN-1").build();
        TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();

        when(timelineUtils.buildPrepareAnalogFailureTimelineElement(
                isNull(), eq(prepareRequestId), eq(failureCause), eq(recIndex), eq(notification)))
                .thenReturn(timelineElement);

        assertDoesNotThrow(() ->
                paperChannelUtils.addPrepareAnalogFailureTimelineElement(
                        null, prepareRequestId, failureCause, recIndex, notification));

        verify(timelineUtils).buildPrepareAnalogFailureTimelineElement(
                isNull(), eq(prepareRequestId), eq(failureCause), eq(recIndex), eq(notification));
        verify(timelineService).addTimelineElement(timelineElement, notification);
    }

    @Test
    void addPrepareAnalogFailureTimelineElement_shouldNotPassFoundAddress_whenBothFieldsAreNull() {
        PhysicalAddressInt foundAddress = PhysicalAddressInt.builder()
                .municipality(null)
                .address(null)
                .build();

        String prepareRequestId = "REQ-1";
        String failureCause = "CAUSE-1";
        Integer recIndex = 1;
        NotificationInt notification = NotificationInt.builder().iun("IUN-1").build();
        TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();

        when(timelineUtils.buildPrepareAnalogFailureTimelineElement(
                isNull(), eq(prepareRequestId), eq(failureCause), eq(recIndex), eq(notification)))
                .thenReturn(timelineElement);

        assertDoesNotThrow(() ->
                paperChannelUtils.addPrepareAnalogFailureTimelineElement(
                        foundAddress, prepareRequestId, failureCause, recIndex, notification));

        verify(timelineUtils).buildPrepareAnalogFailureTimelineElement(
                isNull(), eq(prepareRequestId), eq(failureCause), eq(recIndex), eq(notification));
        verify(timelineService).addTimelineElement(timelineElement, notification);
    }

    @Test
    void addPrepareAnalogFailureTimelineElement_shouldNotPassFoundAddress_whenBothFieldsAreBlank() {
        PhysicalAddressInt foundAddress = PhysicalAddressInt.builder()
                .municipality("")
                .address("")
                .build();

        String prepareRequestId = "REQ-1";
        String failureCause = "CAUSE-1";
        Integer recIndex = 1;
        NotificationInt notification = NotificationInt.builder().iun("IUN-1").build();
        TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();

        when(timelineUtils.buildPrepareAnalogFailureTimelineElement(
                isNull(), eq(prepareRequestId), eq(failureCause), eq(recIndex), eq(notification)))
                .thenReturn(timelineElement);

        assertDoesNotThrow(() ->
                paperChannelUtils.addPrepareAnalogFailureTimelineElement(
                        foundAddress, prepareRequestId, failureCause, recIndex, notification));

        verify(timelineUtils).buildPrepareAnalogFailureTimelineElement(
                isNull(), eq(prepareRequestId), eq(failureCause), eq(recIndex), eq(notification));
        verify(timelineService).addTimelineElement(timelineElement, notification);
    }

    @Test
    void addPrepareAnalogFailureTimelineElement_shouldNotPassFoundAddress_whenMunicipalityIsPresent() {
        PhysicalAddressInt foundAddress = PhysicalAddressInt.builder()
                .municipality("Roma")
                .address(null)
                .build();

        String prepareRequestId = "REQ-1";
        String failureCause = "CAUSE-1";
        Integer recIndex = 1;
        NotificationInt notification = NotificationInt.builder().iun("IUN-1").build();
        TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();

        when(timelineUtils.buildPrepareAnalogFailureTimelineElement(
                isNull(), eq(prepareRequestId), eq(failureCause), eq(recIndex), eq(notification)))
                .thenReturn(timelineElement);

        assertDoesNotThrow(() ->
                paperChannelUtils.addPrepareAnalogFailureTimelineElement(
                        foundAddress, prepareRequestId, failureCause, recIndex, notification));

        verify(timelineUtils).buildPrepareAnalogFailureTimelineElement(
                isNull(), eq(prepareRequestId), eq(failureCause), eq(recIndex), eq(notification));
        verify(timelineService).addTimelineElement(timelineElement, notification);
    }

    @Test
    void addPrepareAnalogFailureTimelineElement_shouldNotPassFoundAddress_whenAddressIsPresent() {
        PhysicalAddressInt foundAddress = PhysicalAddressInt.builder()
                .municipality(null)
                .address("Via Roma 1")
                .build();

        String prepareRequestId = "REQ-1";
        String failureCause = "CAUSE-1";
        Integer recIndex = 1;
        NotificationInt notification = NotificationInt.builder().iun("IUN-1").build();
        TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();

        when(timelineUtils.buildPrepareAnalogFailureTimelineElement(
                isNull(), eq(prepareRequestId), eq(failureCause), eq(recIndex), eq(notification)))
                .thenReturn(timelineElement);

        assertDoesNotThrow(() ->
                paperChannelUtils.addPrepareAnalogFailureTimelineElement(
                        foundAddress, prepareRequestId, failureCause, recIndex, notification));

        verify(timelineUtils).buildPrepareAnalogFailureTimelineElement(
                isNull(), eq(prepareRequestId), eq(failureCause), eq(recIndex), eq(notification));
        verify(timelineService).addTimelineElement(timelineElement, notification);
    }

    @Test
    void addPrepareAnalogFailureTimelineElement_shouldPassFoundAddress_whenBothFieldsArePresent() {
        PhysicalAddressInt foundAddress = PhysicalAddressInt.builder()
                .municipality("Roma")
                .address("Via Roma 1")
                .build();

        String prepareRequestId = "REQ-1";
        String failureCause = "CAUSE-1";
        Integer recIndex = 1;
        NotificationInt notification = NotificationInt.builder().iun("IUN-1").build();
        TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();

        when(timelineUtils.buildPrepareAnalogFailureTimelineElement(
                eq(foundAddress), eq(prepareRequestId), eq(failureCause), eq(recIndex), eq(notification)))
                .thenReturn(timelineElement);

        assertDoesNotThrow(() ->
                paperChannelUtils.addPrepareAnalogFailureTimelineElement(
                        foundAddress, prepareRequestId, failureCause, recIndex, notification));

        verify(timelineUtils).buildPrepareAnalogFailureTimelineElement(
                eq(foundAddress), eq(prepareRequestId), eq(failureCause), eq(recIndex), eq(notification));
        verify(timelineService).addTimelineElement(timelineElement, notification);
    }
}