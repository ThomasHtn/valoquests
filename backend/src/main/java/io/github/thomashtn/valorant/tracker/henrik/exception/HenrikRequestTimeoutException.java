package io.github.thomashtn.valorant.tracker.henrik.exception;

import org.springframework.http.HttpStatus;

/**
 * Indicates that a Henrik or Riot request could not complete before its
 * timeout.
 */
public class HenrikRequestTimeoutException extends HenrikApiException {

    /**
     * Creates a timeout exception from an external HTTP response.
     *
     * @param message external error description
     */
    public HenrikRequestTimeoutException(String message) {
        super(message, HttpStatus.REQUEST_TIMEOUT, true);
    }

    /**
     * Creates a timeout exception from a transport-level failure.
     *
     * @param message application-readable error description
     * @param cause original transport exception
     */
    public HenrikRequestTimeoutException(
        String message,
        Throwable cause
    ) {
        super(message, cause, true);
    }
}
