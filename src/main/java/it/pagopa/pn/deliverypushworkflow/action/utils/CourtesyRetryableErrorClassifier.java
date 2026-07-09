package it.pagopa.pn.deliverypushworkflow.action.utils;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.deliverypushworkflow.dto.address.CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.externalregistry.model.SendMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.OptionalInt;

/**
 * Classifies the outcome of a courtesy message send as transient (retryable) or permanent,
 * according to the allowlist described in the "expected / unexpected courtesy message errors" census.
 *
 * <p>The criterion is a retryable allowlist: only outcomes explicitly recognized as transient are
 * retried; every other outcome (including unclassified ones) is treated as permanent (fail-safe
 * default). Classification looks at the application-level outcome, not the HTTP status alone.</p>
 */
@Component
@Slf4j
public class CourtesyRetryableErrorClassifier {

    /**
     * Classifies a transport error raised while sending on a courtesy channel as retryable.
     * Transient cases are network/timeout errors and the HTTP statuses in the per-channel allowlist
     * (429 and 5xx for every channel, plus 403 for TPP, which maps to an expired token).
     * Any other outcome is permanent.
     */
    public boolean isRetryableTransportError(COURTESY_DIGITAL_ADDRESS_TYPE_INT channel, Throwable error) {
        if (isNetworkError(error)) {
            log.debug("Transport error classified as retryable (network/timeout) for channel={}", channel);
            return true;
        }

        OptionalInt httpStatus = extractHttpStatus(error);
        if (httpStatus.isPresent()) {
            boolean retryable = isRetryableStatus(channel, httpStatus.getAsInt());
            log.debug("Transport error classified as retryable={} (httpStatus={}) for channel={}", retryable, httpStatus.getAsInt(), channel);
            return retryable;
        }

        log.debug("Transport error not recognized, classified as permanent for channel={}", channel);
        return false;
    }

    /**
     * Classifies an App IO application outcome as retryable. Only the outcomes {@code ERROR_USER_STATUS},
     * {@code ERROR_COURTESY} and {@code ERROR_OPTIN} are transient, as they represent technical errors of
     * the downstream App IO service; every other non-sent outcome is permanent.
     */
    public boolean isRetryableIoResult(SendMessageResponse.ResultEnum result) {
        return SendMessageResponse.ResultEnum.ERROR_USER_STATUS.equals(result)
                || SendMessageResponse.ResultEnum.ERROR_COURTESY.equals(result)
                || SendMessageResponse.ResultEnum.ERROR_OPTIN.equals(result);
    }

    private boolean isRetryableStatus(COURTESY_DIGITAL_ADDRESS_TYPE_INT channel, int httpStatus) {
        if (httpStatus == 429) {
            return true;
        }
        if (httpStatus >= 500 && httpStatus <= 599) {
            return true;
        }
        // Only for TPP the 403 maps to an expired OAuth2 token (transient); on the other channels
        // a 403 means an unauthorized client (permanent).
        return channel == COURTESY_DIGITAL_ADDRESS_TYPE_INT.TPP && httpStatus == 403;
    }

    private OptionalInt extractHttpStatus(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof WebClientResponseException webClientResponseException) {
                return OptionalInt.of(webClientResponseException.getStatusCode().value());
            }
            if (current instanceof HttpStatusCodeException httpStatusCodeException) {
                return OptionalInt.of(httpStatusCodeException.getStatusCode().value());
            }
            if (current instanceof PnHttpResponseException pnHttpResponseException && pnHttpResponseException.getStatusCode() > 0) {
                return OptionalInt.of(pnHttpResponseException.getStatusCode());
            }
            current = current.getCause();
        }
        return OptionalInt.empty();
    }

    private boolean isNetworkError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof WebClientRequestException
                    || current instanceof ResourceAccessException
                    || current instanceof SocketTimeoutException
                    || current instanceof UnknownHostException
                    || current instanceof SSLHandshakeException
                    || current instanceof SocketException
                    || current instanceof io.netty.handler.timeout.TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
