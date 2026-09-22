package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.resumepostpayment;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class ResumePostPaymentEventValidator {

    public List<String> validate(ResumePostPaymentEvent event) {
        List<String> errors = new ArrayList<>();
        if (event == null) {
            errors.add("payload is required");
            return errors;
        }
        if (!StringUtils.hasText(event.getIun())) {
            errors.add("iun is required");
        }
        if (event.getRecIndex() == null) {
            errors.add("recIndex is required");
        } else if (event.getRecIndex() < 0) {
            errors.add("recIndex must be non-negative");
        }
        if (event.getResumeType() == null) {
            errors.add("resumeType is required");
        }
        return errors;
    }
}