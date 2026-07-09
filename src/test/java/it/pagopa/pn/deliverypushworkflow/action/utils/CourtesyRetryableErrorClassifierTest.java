package it.pagopa.pn.deliverypushworkflow.action.utils;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry.model.SendMessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourtesyRetryableErrorClassifierTest {

    private final CourtesyRetryableErrorClassifier classifier = new CourtesyRetryableErrorClassifier();

    @Test
    void ioResult_transientErrorsAreRetryable() {
        assertTrue(classifier.isRetryableIoResult(SendMessageResponse.ResultEnum.ERROR_USER_STATUS));
        assertTrue(classifier.isRetryableIoResult(SendMessageResponse.ResultEnum.ERROR_COURTESY));
        assertTrue(classifier.isRetryableIoResult(SendMessageResponse.ResultEnum.ERROR_OPTIN));
    }

    @Test
    void ioResult_permanentOutcomesAreNotRetryable() {
        assertFalse(classifier.isRetryableIoResult(SendMessageResponse.ResultEnum.SENT_COURTESY));
        assertFalse(classifier.isRetryableIoResult(SendMessageResponse.ResultEnum.SENT_OPTIN));
        assertFalse(classifier.isRetryableIoResult(SendMessageResponse.ResultEnum.NOT_SENT_APPIO_UNAVAILABLE));
        assertFalse(classifier.isRetryableIoResult(SendMessageResponse.ResultEnum.NOT_SENT_OPTIN_ALREADY_SENT));
        assertFalse(classifier.isRetryableIoResult(SendMessageResponse.ResultEnum.NOT_SENT_OPTIN_DISABLED_BY_CONF));
        assertFalse(classifier.isRetryableIoResult(SendMessageResponse.ResultEnum.NOT_SENT_COURTESY_DISABLED_BY_CONF));
    }

    @Test
    void transport_networkErrorsAreRetryable() {
        assertTrue(classifier.isRetryableTransportError(COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL, new SocketTimeoutException("timeout")));
        assertTrue(classifier.isRetryableTransportError(COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS, new ResourceAccessException("io error", new SocketTimeoutException())));
    }

    @Test
    void transport_serverErrorsAndTooManyRequestsAreRetryable() {
        assertTrue(classifier.isRetryableTransportError(COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL, webClientResponse(500)));
        assertTrue(classifier.isRetryableTransportError(COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL, webClientResponse(503)));
        assertTrue(classifier.isRetryableTransportError(COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS, webClientResponse(429)));
        assertTrue(classifier.isRetryableTransportError(COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)));
        assertTrue(classifier.isRetryableTransportError(COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO, new PnHttpResponseException("service unavailable", 503)));
    }

    @Test
    void transport_clientErrorsAreNotRetryable() {
        assertFalse(classifier.isRetryableTransportError(COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL, webClientResponse(400)));
        assertFalse(classifier.isRetryableTransportError(COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL, webClientResponse(409)));
        assertFalse(classifier.isRetryableTransportError(COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS, new HttpClientErrorException(HttpStatus.BAD_REQUEST)));
    }

    @Test
    void transport_forbiddenIsRetryableOnlyForTpp() {
        // 403 = expired token on TPP (transient), unauthorized client elsewhere (permanent)
        assertTrue(classifier.isRetryableTransportError(COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, webClientResponse(403)));
        assertFalse(classifier.isRetryableTransportError(COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL, webClientResponse(403)));
        assertFalse(classifier.isRetryableTransportError(COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS, webClientResponse(403)));
    }

    @Test
    void transport_httpStatusIsExtractedFromNestedCause() {
        PnInternalException wrapped = new PnInternalException("error sending EMAIL notification", "ERROR_CODE", webClientResponse(503));
        assertTrue(classifier.isRetryableTransportError(COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL, wrapped));

        PnInternalException wrappedPermanent = new PnInternalException("error sending EMAIL notification", "ERROR_CODE", webClientResponse(400));
        assertFalse(classifier.isRetryableTransportError(COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL, wrappedPermanent));
    }

    @Test
    void transport_unrecognizedErrorIsNotRetryable() {
        // fail-safe default: when the error carries no known HTTP status and is not a network error
        assertFalse(classifier.isRetryableTransportError(COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP, new RuntimeException("unexpected")));
    }

    private static WebClientResponseException webClientResponse(int status) {
        return WebClientResponseException.create(status, "status " + status, HttpHeaders.EMPTY, new byte[0], null);
    }
}
