package it.pagopa.pn.deliverypushworkflow.service.mapper;

import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notificationpaid.NotificationPaidInt;

public class NotificationPaidMapper {

    private NotificationPaidMapper() {}

    public static NotificationPaidInt messageToInternal(PnDeliveryPaymentEvent.Payload paymentEvent) {

        return NotificationPaidInt.builder()
                .iun(paymentEvent.getIun())
                .recipientIdx(paymentEvent.getRecipientIdx())
                .recipientType(NotificationPaidInt.RecipientTypeInt.valueOf(paymentEvent.getRecipientType().getValue()))
                .creditorTaxId(paymentEvent.getCreditorTaxId())
                .noticeCode(paymentEvent.getNoticeCode())
                .paymentDate(paymentEvent.getPaymentDate())
                .uncertainPaymentDate(paymentEvent.isUncertainPaymentDate())
                .paymentType(NotificationPaidInt.PaymentTypeInt.valueOf(paymentEvent.getPaymentType().getValue()))
                .amount(paymentEvent.getAmount())
                .paymentSourceChannel(paymentEvent.getPaymentSourceChannel())
                .build();
    }
}
