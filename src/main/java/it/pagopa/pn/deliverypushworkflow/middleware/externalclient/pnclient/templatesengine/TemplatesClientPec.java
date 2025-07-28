package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.templatesengine;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.templatesengine.model.LanguageEnum;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.templatesengine.model.NotificationAarForPec;

public interface TemplatesClientPec {

    String parametrizedNotificationAarForPec(LanguageEnum language, NotificationAarForPec notificationAarForPec);
}
