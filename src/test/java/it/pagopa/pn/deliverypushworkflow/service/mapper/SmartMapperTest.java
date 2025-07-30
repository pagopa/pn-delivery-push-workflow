package it.pagopa.pn.deliverypushworkflow.service.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypushworkflow.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.DownstreamIdInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendDigitalDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.DigitalAddress;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.DigitalAddressSource;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.SendDigitalDetails;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.TimelineElementDetails;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class SmartMapperTest {
    private SmartMapper smartMapper;
    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        smartMapper = new SmartMapper(objectMapper);
    }

    @Test
    void fromInternalToExternalSendDigitalDetails() {
        SendDigitalDetailsInt sendDigitalDetails = SendDigitalDetailsInt.builder()
                .recIndex(0)
                .digitalAddressSource(DigitalAddressSourceInt.PLATFORM)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .address("testAddress@gmail.com")
                        .build())
                .retryNumber(0)
                .categoryType("SEND_DIGITAL_DOMICILE")
                .downstreamId(DownstreamIdInt.builder()
                        .messageId("messageId")
                        .systemId("systemId")
                        .build())
                .build();

        var details = smartMapper.mapToClassWithObjectMapper(sendDigitalDetails, TimelineElementDetails.class);
        var sendDigitalDetailsExt = (SendDigitalDetails) details;
        Assertions.assertEquals(sendDigitalDetails.getRecIndex(),  sendDigitalDetailsExt.getRecIndex());
        Assertions.assertEquals(sendDigitalDetails.getDigitalAddress().getAddress(), sendDigitalDetailsExt.getDigitalAddress().getAddress() );
    }

    @Test
    void fromExternalToInternalSendDigitalDetails() {
        var timelineElementDetails = new SendDigitalDetails()
                .recIndex(0)
                .digitalAddressSource(DigitalAddressSource.PLATFORM)
                .digitalAddress(new DigitalAddress()
                        .type("PEC")
                        .address("testAddress@gmail.com"))
                .retryNumber(0);

        SendDigitalDetailsInt details = SmartMapper.mapToClass(timelineElementDetails, SendDigitalDetailsInt.class);

        Assertions.assertEquals(timelineElementDetails.getRecIndex(), details.getRecIndex());
        Assertions.assertEquals(timelineElementDetails.getDigitalAddress().getAddress(), details.getDigitalAddress().getAddress() );
    }

    @Test
    void testTimelineElementInternalMappingTransformer(){
        Instant elementTimestamp = Instant.EPOCH.plusMillis(100);

        Instant eventTimestamp = Instant.EPOCH.plusMillis(10);

        TimelineElementInternal source = TimelineElementInternal.builder()
                .elementId("elementid")
                .iun("iun")
                .timestamp(elementTimestamp)
                .details(SendDigitalFeedbackDetailsInt.builder()
                        .notificationDate(eventTimestamp)
                        .build())
                .build();

        TimelineElementInternal ret = SmartMapper.mapToClass(source, TimelineElementInternal.class);

        Assertions.assertNotSame(ret, source);
        Assertions.assertEquals(eventTimestamp, ret.getTimestamp());
    }

    @Test
    void mapToClassWithNullSource() {
        TimelineElementInternal source = null;

        TimelineElementInternal ret = SmartMapper.mapToClass(source, TimelineElementInternal.class);

        Assertions.assertNull(ret);
    }

    @Test
    void mapToClassWithObjectMappperWithNullSource() {
        TimelineElementInternal source = null;

        TimelineElementDetails ret = smartMapper.mapToClassWithObjectMapper(source, TimelineElementDetails.class);

        Assertions.assertNull(ret);
    }
}