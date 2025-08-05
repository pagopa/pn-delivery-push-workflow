package it.pagopa.pn.deliverypushworkflow.service.impl;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.SentNotificationV25;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.NotificationFeePolicy;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

class NotificationServiceImplTest {

    @Mock
    private PnDeliveryClient pnDeliveryClient;

    @Mock
    private PnDeliveryClientReactive pnDeliveryClientReactive;

    private NotificationServiceImpl service;

    @BeforeEach
    public void setup() {
        service = new NotificationServiceImpl(pnDeliveryClient, pnDeliveryClientReactive);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void getNotificationByIun() {
        NotificationInt expected = buildNotificationInt();

        SentNotificationV25 sentNotification = buildSentNotification();
        Mockito.when(pnDeliveryClient.getSentNotification("001")).thenReturn(sentNotification);

        NotificationInt actual = service.getNotificationByIun("001");

        Assertions.assertEquals(expected, actual);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void getNotificationByIunNotFound() {

        Mockito.when(pnDeliveryClient.getSentNotification("001")).thenThrow(PnHttpResponseException.class);

        Assertions.assertThrows(PnHttpResponseException.class, () -> service.getNotificationByIun("001"));

    }
    
    @Test
    @ExtendWith(SpringExtension.class)
    void getRecipientsQuickAccessLinkToken() {
        Map<String, String> expected = Map.of("internalId","token");

        Mockito.when(pnDeliveryClient.getQuickAccessLinkTokensPrivate("001")).thenReturn(expected);

        Map<String, String> actual = service.getRecipientsQuickAccessLinkToken("001");

        Assertions.assertEquals(expected, actual);
    }
    
    @Test
    @ExtendWith(SpringExtension.class)
    void getRecipientsQuickAccessLinkTokenFailure() {       
        Mockito.when(pnDeliveryClient.getQuickAccessLinkTokensPrivate("001"))
        .thenThrow(PnHttpResponseException.class);
        Assertions.assertThrows(PnHttpResponseException.class, () -> service.getRecipientsQuickAccessLinkToken("001"));
        
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void removeAllNotificationCostsByIun() {
        Mockito.when(pnDeliveryClientReactive.removeAllNotificationCostsByIun("001"))
                .thenReturn(Mono.empty());

        Mono<Void> mono = service.removeAllNotificationCostsByIun("001");
        Assertions.assertDoesNotThrow( () -> mono.block());

    }

    @Test
    @ExtendWith(SpringExtension.class)
    void removeAllNotificationCostsByIunError() {
        Mockito.when(pnDeliveryClientReactive.removeAllNotificationCostsByIun("001"))
                .thenReturn(Mono.error(new PnHttpResponseException("", 400)));

        Mono<Void> mono = service.removeAllNotificationCostsByIun("001");
        Assertions.assertThrows(PnInternalException.class, mono::block);

    }
    
    private SentNotificationV25 buildSentNotification() {
        SentNotificationV25 sentNotification = new SentNotificationV25();
        sentNotification.setIun("001");
        sentNotification.setPhysicalCommunicationType(SentNotificationV25.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890);
        sentNotification.setNotificationFeePolicy(it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.NotificationFeePolicy.DELIVERY_MODE);
        return sentNotification;
    }
    
    private NotificationInt buildNotificationInt() {
        return NotificationInt.builder()
                .iun("001")
                .recipients(Collections.emptyList())
                .documents(Collections.emptyList())
                .sender(NotificationSenderInt.builder().build())
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .physicalCommunicationType(ServiceLevelTypeInt.REGISTERED_LETTER_890)
                .build();
    }
}