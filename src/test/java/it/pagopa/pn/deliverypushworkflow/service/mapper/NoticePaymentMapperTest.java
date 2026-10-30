package it.pagopa.pn.deliverypushworkflow.service.mapper;

import it.pagopa.pn.api.dto.events.PnNoticePaymentPaidEvent;
import it.pagopa.pn.deliverypushworkflow.dto.cost.NoticePaymentInt;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NoticePaymentMapperTest {

    @Test
    void mapsAllNoticePaymentFields() {
        PnNoticePaymentPaidEvent paymentEvent = mock(PnNoticePaymentPaidEvent.class);
        PnNoticePaymentPaidEvent.Payload payload = mock(PnNoticePaymentPaidEvent.Payload.class);
        Instant paymentDateTime = Instant.parse("2026-10-29T10:00:00Z");
        Instant dueDate = Instant.parse("2026-11-29T23:59:59Z");
        Instant persistedTimestamp = Instant.parse("2026-10-29T10:01:00Z");
        Instant publishedTimestamp = Instant.parse("2026-10-29T10:02:00Z");
        Instant ingestionTimestamp = Instant.parse("2026-10-29T10:03:00Z");

        when(paymentEvent.getPayload()).thenReturn(payload);
        when(payload.getModelType()).thenReturn("model");
        when(payload.getIuv()).thenReturn("iuv");
        when(payload.getIur()).thenReturn("iur");
        when(payload.getNoticeCode()).thenReturn("notice-code");
        when(payload.getCreditorTaxId()).thenReturn("tax-id");
        when(payload.getCreditorCompanyName()).thenReturn("creditor");
        when(payload.getPaymentDateTime()).thenReturn(paymentDateTime);
        when(payload.getDueDate()).thenReturn(dueDate);
        when(payload.getTotalNotice()).thenReturn(2);
        when(payload.getPaymentSourceChannel()).thenReturn("channel");
        when(payload.getAmount()).thenReturn(1200);
        when(payload.getPaymentMethod()).thenReturn("payment-method");
        when(payload.getFee()).thenReturn("100");
        when(payload.getPaymentId()).thenReturn("payment-id");
        when(payload.getPagopaPersistedTimestamp()).thenReturn(persistedTimestamp);
        when(payload.getPagopaPublishedTimestamp()).thenReturn(publishedTimestamp);
        when(payload.getSendIngestionTimestamp()).thenReturn(ingestionTimestamp);
        when(payload.getKafkaPartition()).thenReturn("partition");
        when(payload.getIun()).thenReturn("iun");
        when(payload.getRecIndex()).thenReturn(1);

        NoticePaymentInt actual = NoticePaymentMapper.messageToInternal(paymentEvent);

        assertEquals(NoticePaymentInt.builder()
                .modelType("model")
                .iuv("iuv")
                .iur("iur")
                .noticeCode("notice-code")
                .creditorTaxId("tax-id")
                .creditorCompanyName("creditor")
                .paymentDateTime(paymentDateTime)
                .dueDate(dueDate)
                .totalNotice(2)
                .paymentSourceChannel("channel")
                .amount(1200)
                .paymentMethod("payment-method")
                .fee("100")
                .paymentId("payment-id")
                .pagopaPersistedTimestamp(persistedTimestamp)
                .pagopaPublishedTimestamp(publishedTimestamp)
                .sendIngestionTimestamp(ingestionTimestamp)
                .kafkaPartition("partition")
                .iun("iun")
                .recIndex(1)
                .build(), actual);
    }
}
