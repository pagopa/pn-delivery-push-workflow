package it.pagopa.pn.deliverypushworkflow.dto.courtesy;

/**
 * Classified outcome of a single courtesy message send attempt on a channel.
 * The distinction between {@link #RETRYABLE_ERROR} and {@link #PERMANENT_FAILURE} follows the
 * retryable allowlist described in the courtesy errors census: only outcomes explicitly recognized
 * as transient are retried, every other outcome closes the channel (fail-safe default: when in doubt,
 * do not retry).
 */
public enum CourtesySendOutcome {
    /** Send (or take-over) succeeded. */
    SENT,
    /** Recognized transient error: retrying makes sense. */
    RETRYABLE_ERROR,
    /** Permanent or unclassified outcome: the channel is closed without further attempts. */
    PERMANENT_FAILURE
}
