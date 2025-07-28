package it.pagopa.pn.deliverypushworkflow.action.it.mockbean;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.templatesengine.model.LanguageEnum;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.templatesengine.model.NotificationAarForPec;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.templatesengine.TemplatesClientPec;

public class TemplatesClientMockPec implements TemplatesClientPec {
    private static final String RESULT_STRING = "Templates As String Result";

    @Override
    public String parametrizedNotificationAarForPec(LanguageEnum xLanguage, NotificationAarForPec notificationAarForPec) {
        return RESULT_STRING;
    }
}
