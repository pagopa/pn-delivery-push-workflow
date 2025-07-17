package it.pagopa.pn.deliverypushworkflow.middleware.pnclient.templatesengine;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.templatesengine.model.LanguageEnumDto;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.templatesengine.model.NotificationAarForPecDto;

public interface TemplatesClientPec {

    String parametrizedNotificationAarForPec(LanguageEnumDto language, NotificationAarForPecDto notificationAarForPec);
}
