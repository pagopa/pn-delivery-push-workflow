package it.pagopa.pn.deliverypushworkflow.dto.courtesy;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WhatsappTemplateDescriptorTest {

    @Test
    void fromJsonOk() {
        WhatsappTemplateDescriptor descriptor = WhatsappTemplateDescriptor.fromJson(
                "{\"templateName\":\"send_avviso_notifica_analogica\",\"language\":\"it\",\"bodyParameters\":[\"Ente\",\"IUN\"],"
                        + "\"previewText\":\"Hai ricevuto una notifica\",\"unknownField\":true}");
        assertEquals("send_avviso_notifica_analogica", descriptor.getTemplateName());
        assertEquals("it", descriptor.getLanguage());
        assertEquals(List.of("Ente", "IUN"), descriptor.getBodyParameters());
        assertNull(descriptor.getButtonParameters());
        assertEquals("Hai ricevuto una notifica", descriptor.getPreviewText());
    }

    @Test
    void fromJsonWithLeadingWhitespaceFromFreemarker() {
        WhatsappTemplateDescriptor descriptor = WhatsappTemplateDescriptor.fromJson("\n\n{\"templateName\":\"t\",\"language\":\"it\"}\n");
        assertEquals("t", descriptor.getTemplateName());
    }

    @Test
    void fromJsonMissingTemplateNameFails() {
        assertThrows(PnInternalException.class, () -> WhatsappTemplateDescriptor.fromJson("{\"language\":\"it\"}"));
    }

    @Test
    void fromJsonInvalidFails() {
        assertThrows(PnInternalException.class, () -> WhatsappTemplateDescriptor.fromJson("Hai ricevuto una notifica"));
        assertThrows(PnInternalException.class, () -> WhatsappTemplateDescriptor.fromJson(null));
    }

    @Test
    void toStringDoesNotLeakParameters() {
        WhatsappTemplateDescriptor descriptor = WhatsappTemplateDescriptor.builder()
                .templateName("t").language("it").bodyParameters(List.of("12345")).previewText("codice 12345").build();
        assertFalse(descriptor.toString().contains("12345"));
    }
}
