package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.resumepostpayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumePostPaymentEvent {
    private String iun;
    private Integer recIndex;
    private ResumeType resumeType;
}