package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.notificationcostservice;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.notificationcostservice_reactive.api.PaperCostApi;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.notificationcostservice_reactive.model.AnalogUpdateCostPhase;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.notificationcostservice_reactive.model.PaperCostToInvalidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class NotificationCostServiceClientImplTest {

    private PaperCostApi paperCostApi;
    private NotificationCostServiceClientImpl client;

    @BeforeEach
    void setUp() {
        this.paperCostApi = Mockito.mock(PaperCostApi.class);
        this.client = new NotificationCostServiceClientImpl(paperCostApi);
    }

    @Test
    void invalidatePaperCostWithHttpInfo_skipsApiCallWhenCostPhasesAreEmpty() {
        PaperCostToInvalidate request = new PaperCostToInvalidate();
        request.setCostPhases(new ArrayList<>());

        StepVerifier.create(client.invalidatePaperCostWithHttpInfo("IUN_1", request))
                .assertNext(response -> {
                    org.junit.jupiter.api.Assertions.assertEquals(204, response.getStatusCode().value());
                    org.junit.jupiter.api.Assertions.assertFalse(response.hasBody());
                })
                .verifyComplete();

        Mockito.verify(paperCostApi, Mockito.never()).invalidatePaperCostWithHttpInfo(any(), any());
    }

    @Test
    void invalidatePaperCostWithHttpInfo_callsApiWhenCostPhasesArePresent() {
        PaperCostToInvalidate request = new PaperCostToInvalidate();
        request.setCostPhases(new ArrayList<>(java.util.List.of(AnalogUpdateCostPhase.SEND_ANALOG_DOMICILE_ATTEMPT_1)));

        Mockito.when(paperCostApi.invalidatePaperCostWithHttpInfo(eq("IUN_1"), any(PaperCostToInvalidate.class)))
                .thenReturn(Mono.just(org.springframework.http.ResponseEntity.ok().build()));

        StepVerifier.create(client.invalidatePaperCostWithHttpInfo("IUN_1", request))
                .assertNext(response -> org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value()))
                .verifyComplete();

        Mockito.verify(paperCostApi).invalidatePaperCostWithHttpInfo(eq("IUN_1"), any(PaperCostToInvalidate.class));
    }
}
