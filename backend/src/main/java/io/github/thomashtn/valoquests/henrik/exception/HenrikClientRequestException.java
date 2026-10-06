package io.github.thomashtn.valoquests.henrik.exception;


/**
 * Indicates that Henrik rejected a request that should not automatically be
 * retried.
 */
public class HenrikClientRequestException extends HenrikApiException {

    /**
     * Creates a non-retryable client-request exception.
     *
     * @param message external error description
     */
    public HenrikClientRequestException(String message) {
        super(message, false);
    }
}
