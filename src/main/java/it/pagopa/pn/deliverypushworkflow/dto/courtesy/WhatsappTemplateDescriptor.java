package it.pagopa.pn.deliverypushworkflow.dto.courtesy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.StringUtils;

import java.util.List;

import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALID_TEMPLATE;

/**
 * Descrittore del template WhatsApp restituito da pn-templates-engine
 * (template NotificationAARForWhatsAppDigital / NotificationAARForWhatsAppAnalog).
 * Il testo del messaggio è approvato su Meta: SEND invia solo nome, lingua e parametri.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsappTemplateDescriptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String templateName;
    private String language;
    @ToString.Exclude
    private List<String> bodyParameters;
    @ToString.Exclude
    private List<String> buttonParameters;
    @ToString.Exclude
    private String previewText;

    public static WhatsappTemplateDescriptor fromJson(String json) {
        try {
            WhatsappTemplateDescriptor descriptor = OBJECT_MAPPER.readValue(json, WhatsappTemplateDescriptor.class);
            if (!StringUtils.hasText(descriptor.getTemplateName()) || !StringUtils.hasText(descriptor.getLanguage())) {
                throw new PnInternalException("WhatsApp template descriptor without templateName or language", ERROR_CODE_DELIVERYPUSH_INVALID_TEMPLATE);
            }
            return descriptor;
        } catch (PnInternalException e) {
            throw e;
        } catch (Exception e) {
            throw new PnInternalException("Invalid WhatsApp template descriptor", ERROR_CODE_DELIVERYPUSH_INVALID_TEMPLATE, e);
        }
    }
}
