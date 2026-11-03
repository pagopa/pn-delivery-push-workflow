package it.pagopa.pn.deliverypushworkflow.service.mapper;

import it.pagopa.pn.api.dto.events.PnNoticePaymentPaidEvent;
import it.pagopa.pn.deliverypushworkflow.dto.cost.NoticePaymentInt;

public class NoticePaymentMapper {

    private NoticePaymentMapper(){}

    public static NoticePaymentInt messageToInternal(PnNoticePaymentPaidEvent paymentEvent) {
        PnNoticePaymentPaidEvent.Payload payload = paymentEvent.getPayload();

        return NoticePaymentInt.builder()
                .modelType(payload.getModelType())
                .iuv(payload.getIuv())
                .iur(payload.getIur())
                .noticeCode(payload.getNoticeCode())
                .creditorTaxId(payload.getCreditorTaxId())
                .creditorCompanyName(payload.getCreditorCompanyName())
                .paymentDateTime(payload.getPaymentDateTime())
                .dueDate(payload.getDueDate())
                .totalNotice(payload.getTotalNotice())
                .paymentSourceChannel(payload.getPaymentSourceChannel())
                .amount(payload.getAmount())
                .paymentMethod(payload.getPaymentMethod())
                .fee(payload.getFee())
                .paymentId(payload.getPaymentId())
                .pagopaPersistedTimestamp(payload.getPagopaPersistedTimestamp())
                .pagopaPublishedTimestamp(payload.getPagopaPublishedTimestamp())
                .sendIngestionTimestamp(payload.getSendIngestionTimestamp())
                .kafkaPartition(payload.getKafkaPartition())
                .communicationType(payload.getCommunicationType())
                .iun(payload.getIun())
                .recIndex(payload.getRecIndex())
                .build();
    }
}
