package it.pagopa.pn.deliverypushworkflow.middleware.responsehandler;

import it.pagopa.pn.api.dto.events.PnNoticePaymentPaidEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NoticePaymentPaidEventValidatorTest {

    private final NoticePaymentPaidEventValidator validator = new NoticePaymentPaidEventValidator();

    @Test
    void returnsNoErrorsWhenRequiredFieldsAreValid() {
        PnNoticePaymentPaidEvent event = noticePaymentEvent("iun-123", 0, "notice-code", "tax-id");

        assertEquals(List.of(), validator.validate(event));
    }

    @Test
    void reportsMissingPayload() {
        PnNoticePaymentPaidEvent event = mock(PnNoticePaymentPaidEvent.class);
        when(event.getPayload()).thenReturn(null);

        assertEquals(List.of("payload is required"), validator.validate(event));
    }

    @Test
    void reportsMissingRequiredFields() {
        PnNoticePaymentPaidEvent event = noticePaymentEvent(null, null, "  ", null);

        assertEquals(List.of(
                "iun is required",
                "recIndex is required",
                "noticeCode is required",
                "creditorTaxId is required"
        ), validator.validate(event));
    }

    @Test
    void reportsNegativeRecIndex() {
        PnNoticePaymentPaidEvent event = noticePaymentEvent("iun-123", -1, "notice-code", "tax-id");

        assertEquals(List.of("recIndex must be non-negative"), validator.validate(event));
    }

    private PnNoticePaymentPaidEvent noticePaymentEvent(
            String iun, Integer recIndex, String noticeCode, String creditorTaxId) {
        PnNoticePaymentPaidEvent event = mock(PnNoticePaymentPaidEvent.class);
        PnNoticePaymentPaidEvent.Payload payload = mock(PnNoticePaymentPaidEvent.Payload.class);
        when(event.getPayload()).thenReturn(payload);
        when(payload.getIun()).thenReturn(iun);
        when(payload.getRecIndex()).thenReturn(recIndex);
        when(payload.getNoticeCode()).thenReturn(noticeCode);
        when(payload.getCreditorTaxId()).thenReturn(creditorTaxId);
        return event;
    }
}
