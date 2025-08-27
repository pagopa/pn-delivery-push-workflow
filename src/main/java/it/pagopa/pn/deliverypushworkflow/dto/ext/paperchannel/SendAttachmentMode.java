package it.pagopa.pn.deliverypushworkflow.dto.ext.paperchannel;

import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
public enum SendAttachmentMode {
    AAR("AAR"),
    AAR_DOCUMENTS("AAR-DOCUMENTS"),
    AAR_DOCUMENTS_PAYMENTS("AAR-DOCUMENTS-PAYMENTS");

    public static SendAttachmentMode fromValue(String sendAnalogNotificationAttachments) {
        if (StringUtils.hasText(sendAnalogNotificationAttachments)) {
            try {
                SendAttachmentMode mode;
                if ("AAR-DOCUMENTS-PAYMENTS".equals(sendAnalogNotificationAttachments)) {
                    mode = AAR_DOCUMENTS_PAYMENTS;
                } else if ("AAR-DOCUMENTS".equals(sendAnalogNotificationAttachments)) {
                    mode = AAR_DOCUMENTS;
                } else {
                    mode = SendAttachmentMode.valueOf(sendAnalogNotificationAttachments);
                }
                return mode;
            } catch (Exception e) {
                return AAR;
            }
        }
        return AAR;
    }

    private final String value;

    SendAttachmentMode(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
