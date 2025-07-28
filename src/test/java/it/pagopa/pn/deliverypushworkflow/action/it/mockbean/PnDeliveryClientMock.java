package it.pagopa.pn.deliverypushworkflow.action.it.mockbean;

import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.NotificationRecipientV24;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.SentNotificationV25;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypushworkflow.service.mapper.NotificationMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
public class PnDeliveryClientMock implements PnDeliveryClient {
    private CopyOnWriteArrayList<SentNotificationV25> notifications;

    public SentNotificationV25 getNotification(String iun) {
        return this.notifications.stream()
                .filter(notification -> iun.equals(notification.getIun()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Test error, iun is not present in getNotification IUN:" + iun));
    }

    public void clear() {
        this.notifications = new CopyOnWriteArrayList<>();
    }

    public void addNotification(NotificationInt notification) {
        SentNotificationV25 sentNotification = NotificationMapper.internalToExternal(notification);
        this.notifications.add(sentNotification);
        log.info("ADDED_IUN:" + notification.getIun());
    }

    @Override
    public SentNotificationV25 getSentNotification(String iun) {
        Optional<SentNotificationV25> sentNotificationOpt = notifications.stream().filter(notification -> iun.equals(notification.getIun())).findFirst();
        if(sentNotificationOpt.isPresent()){
            return sentNotificationOpt.get();
        }
        throw new RuntimeException("Test error, iun is not presente in getSentNotification IUN:" + iun);
    }

    @Override
    public Map<String, String> getQuickAccessLinkTokensPrivate(String iun) {
        return this.notifications.stream()
        .filter(n->n.getIun().equals(iun))
        .map(SentNotificationV25::getRecipients)
        .flatMap(List::stream)
        .collect(Collectors.toMap(NotificationRecipientV24::getInternalId, (n) -> "test"));
    }
}
