package it.pagopa.pn.deliverypushworkflow.action.it.mockbean;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.templatesengine.model.*;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.templatesengine.TemplatesClient;

import java.io.IOException;

public class TemplatesClientMock implements TemplatesClient {

    private static final String RESULT_STRING = "Templates As String Result";

    @Override
    public byte[] notificationViewedLegalFact(LanguageEnum xLanguage, NotificationViewedLegalFact notificationViewedLegalFact) {
        return resultPdf();
    }

    @Override
    public byte[] pecDeliveryWorkflowLegalFact(LanguageEnum xLanguage, PecDeliveryWorkflowLegalFact pecDeliveryWorkflowLegalFact) {
        return resultPdf();
    }

    @Override
    public byte[] analogDeliveryWorkflowFailureLegalFact(LanguageEnum xLanguage, AnalogDeliveryWorkflowFailureLegalFact analogDeliveryWorkflowFailureLegalFact) {
        return resultPdf();
    }

    @Override
    public byte[] notificationCancelledLegalFact(LanguageEnum xLanguage, NotificationCancelledLegalFact notificationCancelledLegalFact) {
        return resultPdf();
    }

    @Override
    public byte[] notificationAar(LanguageEnum xLanguage, NotificationAar notificationAar) {
        return resultPdf();
    }

    @Override
    public byte[] notificationAarRaddAlt(LanguageEnum xLanguage, NotificationAarRaddAlt notificationAarRaddAlt) {
        return resultPdf();
    }

    @Override
    public String notificationAarForSubject(LanguageEnum xLanguage, NotificationAarForSubject notificationAarForSubject) {
        return RESULT_STRING;
    }

    @Override
    public String notificationAarForSmsAnalog(LanguageEnum xLanguage, NotificationAarForSms notificationAarForSms) {
        return RESULT_STRING;
    }

    @Override
    public String notificationAarForSmsDigital(LanguageEnum xLanguage, NotificationAarForSms notificationAarForSms) {
        return RESULT_STRING;
    }

    @Override
    public String notificationAarForEmailAnalog(LanguageEnum xLanguage, NotificationAarForEmail notificationAarForEmail) {
        return RESULT_STRING;
    }

    @Override
    public String notificationAarForEmailDigital(LanguageEnum xLanguage, NotificationAarForEmail notificationAarForEmail) {
        return RESULT_STRING;
    }

    @Override
    public byte[] analogDeliveryWorkflowTimeoutLegalFact(LanguageEnum xLanguage, AnalogDeliveryWorkflowTimeoutLegalFact analogDeliveryWorkflowTimeoutLegalFact) {
        return resultPdf();
    }

    private byte[] resultPdf() {
        try (var result = this.getClass().getResourceAsStream("/pdf/response.pdf")) {
            if (result == null) {
                throw new PnInternalException("resultPdf", "resultPdf no pdf found");
            }
            return result.readAllBytes();
        } catch (IOException ex) {
            throw new PnInternalException(ex.getMessage(), ex.getLocalizedMessage());
        }
    }
}
