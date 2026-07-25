package io.github.thomashtn.valorant.tracker.henrik.exception;

import org.springframework.http.HttpStatusCode;

/**
 * Indicates that Henrik rejected a request that should not automatically be
 * retried.
 */
public class HenrikClientRequestException extends HenrikApiException {

    /**
     * Creates a non-retryable client-request exception.
     *
     * @param message external error description
     * @param statusCode external HTTP status
     */
    public HenrikClientRequestException(
        String message,
        HttpStatusCode statusCode
    ) {
        super(message, statusCode, false);
    }
}
