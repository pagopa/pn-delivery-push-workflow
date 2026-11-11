package it.pagopa.pn.deliverypushworkflow.dto.cost;

import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.CommunicationType;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NoticePaymentInt {
    private String modelType;
    private String iuv;
    private String iur;
    private String noticeCode;
    private String creditorTaxId;
    private String creditorCompanyName;
    private Instant paymentDateTime;
    private Instant dueDate;
    private Integer totalNotice;
    private String paymentSourceChannel;
    private Integer amount;
    private String paymentMethod;
    private String fee;
    private String paymentId;
    private Instant pagopaPersistedTimestamp;
    private Instant pagopaPublishedTimestamp;
    private Instant sendIngestionTimestamp;
    private String kafkaPartition;
    private CommunicationType communicationType;
    private String iun;
    private Integer recIndex;
}
