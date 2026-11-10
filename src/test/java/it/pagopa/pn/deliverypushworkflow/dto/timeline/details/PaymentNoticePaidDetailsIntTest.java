package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import it.pagopa.pn.deliverypushworkflow.utils.AuditLogUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentNoticePaidDetailsIntTest {

    @Test
    void getElementTimestampReturnsEventTimestamp() {
        Instant eventTimestamp = Instant.parse("2026-10-28T12:00:00Z");
        PaymentNoticePaidDetailsInt details = PaymentNoticePaidDetailsInt.builder()
                .eventTimestamp(eventTimestamp)
                .build();

        assertEquals(eventTimestamp, details.getElementTimestamp());
    }

    @Test
    void toLogRedactsSensitivePaymentAndPersonalData() {
        PaymentNoticePaidDetailsInt details = PaymentNoticePaidDetailsInt.builder()
                .recIndex(1)
                .recipientType("PF")
                .iuv("sensitive-iuv")
                .noticeCode("notice-code")
                .creditorTaxId("creditor-tax-id")
                .companyName("sensitive-company")
                .amount(1000)
                .fee(100)
                .eventId("sensitive-event-id")
                .build();

        String log = details.toLog();

        assertTrue(log.contains("recIndex=1"));
        assertTrue(log.contains("recipientType=PF"));
        assertTrue(log.contains("iuv=" + AuditLogUtils.SENSITIVE));
        assertTrue(log.contains("noticeCode=notice-code"));
        assertTrue(log.contains("creditorTaxId=creditor-tax-id"));
        assertTrue(log.contains("companyName=" + AuditLogUtils.SENSITIVE));
        assertTrue(log.contains("amount=" + AuditLogUtils.SENSITIVE));
        assertTrue(log.contains("fee=" + AuditLogUtils.SENSITIVE));
        assertTrue(log.contains("eventId=" + AuditLogUtils.SENSITIVE));
        assertFalse(log.contains("sensitive-"));
    }
}
