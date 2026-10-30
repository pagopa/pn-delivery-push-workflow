package it.pagopa.pn.deliverypushworkflow.middleware.responsehandler;

import it.pagopa.pn.api.dto.events.PnNoticePaymentPaidEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class NoticePaymentPaidEventValidator {

    public List<String> validate(PnNoticePaymentPaidEvent event) {
        PnNoticePaymentPaidEvent.Payload payload = event.getPayload();
        List<String> errors = new ArrayList<>();
        if (payload == null) {
            errors.add("payload is required");
            return errors;
        }
        if (!StringUtils.hasText(payload.getIun())) {
            errors.add("iun is required");
        }
        if (payload.getRecIndex() == null) {
            errors.add("recIndex is required");
        } else if (payload.getRecIndex() < 0) {
            errors.add("recIndex must be non-negative");
        }
        if (!StringUtils.hasText(payload.getNoticeCode())) {
            errors.add("noticeCode is required");
        }
        if (!StringUtils.hasText(payload.getCreditorTaxId())) {
            errors.add("creditorTaxId is required");
        }
        return errors;
    }
}