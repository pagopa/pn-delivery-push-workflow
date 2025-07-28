package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.nationalregistries;

import it.pagopa.pn.commons.log.PnLogger;

import java.time.Instant;

public interface NationalRegistriesClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_NATIONAL_REGISTRIES;
    String GET_DIGITAL_GENERAL_ADDRESS = "GET DIGITAL GENERAL ADDRESS";

    void sendRequestForGetDigitalAddress(String taxId, String recipientType, String correlationId, Instant notificationSentAt);
}
 