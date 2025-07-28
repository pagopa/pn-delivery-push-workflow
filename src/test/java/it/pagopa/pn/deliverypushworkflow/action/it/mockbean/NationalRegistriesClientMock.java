package it.pagopa.pn.deliverypushworkflow.action.it.mockbean;

import it.pagopa.pn.deliverypushworkflow.action.it.utils.MethodExecutor;
import it.pagopa.pn.deliverypushworkflow.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventIdBuilder;
import it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.nationalregistries.NationalRegistriesClient;
import it.pagopa.pn.deliverypushworkflow.middleware.responsehandler.NationalRegistriesResponseHandler;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import it.pagopa.pn.deliverypushworkflow.utils.ThreadPool;
import org.junit.jupiter.api.Assertions;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class NationalRegistriesClientMock implements NationalRegistriesClient {

    private int getNationalRegistriesCalledTimes = 0;

    private final NationalRegistriesResponseHandler nationalRegistriesResponseHandler;
    private ConcurrentMap<String, LegalDigitalAddressInt> digitalAddressResponse;
    private ConcurrentMap<String, LegalDigitalAddressInt> digitalAddressResponseSecondCycle;
    private final TimelineService timelineService;


    public NationalRegistriesClientMock(
            NationalRegistriesResponseHandler nationalRegistriesResponseHandler,
            TimelineService timelineService
    ) {
        this.nationalRegistriesResponseHandler = nationalRegistriesResponseHandler;
        this.timelineService = timelineService;
    }

    public void clear() {
        this.digitalAddressResponse = new ConcurrentHashMap<>();
        this.digitalAddressResponseSecondCycle = new ConcurrentHashMap<>();
        this.getNationalRegistriesCalledTimes = 0;
    }

    public void addDigital(String key, LegalDigitalAddressInt value) {
        this.digitalAddressResponse.put(key,value);
        this.digitalAddressResponseSecondCycle.put(key,value);
    }

    public void addDigitalSecondCycle(String key, LegalDigitalAddressInt value) {
        this.digitalAddressResponseSecondCycle.put(key,value);
    }
    
    @Override
    public void sendRequestForGetDigitalAddress(String taxId, String recipientType, String correlationId, Instant notificationSentAt) {
        ThreadPool.start( new Thread(() -> {
            // Viene atteso fino a che l'elemento di timeline relativo all'invio verso extChannel sia stato inserito
            //timelineEventId = <CATEGORY_VALUE>;IUN_<IUN_VALUE>;RECINDEX_<RECINDEX_VALUE>
            String iunFromElementId = correlationId.split("\\" + TimelineEventIdBuilder.DELIMITER)[1];
            String iun = iunFromElementId.replace("IUN_", "");

            MethodExecutor.waitForExecution(
                    () -> timelineService.getTimelineElement(iun, correlationId)
            );

            Assertions.assertDoesNotThrow(() -> simulateDigitalAddressResponse(taxId, correlationId));
        }));
    }

    private void simulateDigitalAddressResponse(String taxId, String correlationId) {
        LegalDigitalAddressInt address;

        if(getNationalRegistriesCalledTimes == 0){
            address = this.digitalAddressResponse.get(taxId);
        } else {
            address = this.digitalAddressResponseSecondCycle.get(taxId);
        }

        getNationalRegistriesCalledTimes += 1;
        
        NationalRegistriesResponse response = NationalRegistriesResponse.builder()
                .correlationId(correlationId)
                .digitalAddress(address)
                .build();
        nationalRegistriesResponseHandler.handleResponse(response);
    }
}
