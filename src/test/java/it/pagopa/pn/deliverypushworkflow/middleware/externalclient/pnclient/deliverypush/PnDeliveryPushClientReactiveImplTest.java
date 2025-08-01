package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.deliverypush;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.deliverypush.api.NotificationProcessCostApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.deliverypush.model.NotificationProcessCostResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.delivery.model.NotificationFeePolicy.DELIVERY_MODE;
import static org.mockito.ArgumentMatchers.any;

class PnDeliveryPushClientReactiveImplTest {
    private NotificationProcessCostApi notificationProcessCostApi;

    private PnDeliveryPushClientReactiveImpl client;

    @BeforeEach
    void setup() {
        this.notificationProcessCostApi = Mockito.mock(NotificationProcessCostApi.class);
        client = new PnDeliveryPushClientReactiveImpl(notificationProcessCostApi);
    }

    @Test
    void getNotificationProcessCost() {
        NotificationProcessCostResponse notificationProcessCostResponse = new NotificationProcessCostResponse();
        Mockito.when(notificationProcessCostApi.notificationProcessCost(any(), any(), any(), any(), any() ,any()))
                .thenReturn(Mono.just(notificationProcessCostResponse));

        Mono<NotificationProcessCostResponse> response = client.getNotificationProcessCost(
                "testIun",
                0,
                DELIVERY_MODE,
                true,
                100,
                22
        );

        StepVerifier.create(response)
                .assertNext(Assertions::assertNotNull)
                .verifyComplete();

        Mockito.verify(notificationProcessCostApi).notificationProcessCost(
                "testIun",
                0,
                it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.deliverypush.model.NotificationFeePolicy.DELIVERY_MODE,
                true,
                100,
                22
        );
    }
}