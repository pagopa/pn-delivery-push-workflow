package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.resumepostpayment;

public interface ResumePostPaymentHandler {
    void handle(ResumePostPaymentEvent event);
}