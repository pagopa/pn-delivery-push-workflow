package it.pagopa.pn.deliverypushworkflow.action.utils;

import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.PublicRegistryValidationResponseDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.ReceiverAddressSourceTypeInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @ParameterizedTest
    @NullSource
    @EnumSource(value = ReceiverAddressSourceTypeInt.class, names = {"ANPR", "REGISTRO_IMPRESE"})
    void resolveAddressSourceUsesRegistryOrMittenteWhenValidationIsAbsent(
            ReceiverAddressSourceTypeInt registry) {
        String iun = "IUN-1";
        int recIndex = 1;
        Optional<TimelineElementInternal> validationResponse = registry == null
                ? Optional.empty()
                : Optional.of(TimelineElementInternal.builder()
                        .category(TimelineElementCategoryInt.PUBLIC_REGISTRY_VALIDATION_RESPONSE)
                        .details(PublicRegistryValidationResponseDetailsInt.builder()
                                .recIndex(recIndex)
                                .registry(registry.getValue())
                                .build())
                        .build());
        when(timelineService.getTimelineElementForSpecificRecipient(
                iun, recIndex, TimelineElementCategoryInt.PUBLIC_REGISTRY_VALIDATION_RESPONSE))
                .thenReturn(validationResponse);

        ReceiverAddressSourceTypeInt actual = paperChannelUtils.resolveAddressSource(iun, recIndex);

        assertEquals(registry == null ? ReceiverAddressSourceTypeInt.MITTENTE : registry, actual);
        verify(timelineService).getTimelineElementForSpecificRecipient(
                iun, recIndex, TimelineElementCategoryInt.PUBLIC_REGISTRY_VALIDATION_RESPONSE);
        verifyNoMoreInteractions(timelineService);
        verifyNoInteractions(timelineUtils);
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