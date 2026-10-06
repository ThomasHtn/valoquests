package io.github.thomashtn.valoquests.henrik.exception;

import lombok.Getter;

/**
 * Base exception for errors occurring while communicating with HenrikDev.
 */
@Getter
public class HenrikApiException extends RuntimeException {

    /**
     * Indicates whether another attempt may reasonably succeed.
     */
    private final boolean retryable;

    /**
     * Creates an exception representing an HTTP response returned by Henrik.
     *
     * @param message application-readable error message
     * @param retryable whether the operation can be retried
     */
    public HenrikApiException(
        String message,
        boolean retryable
    ) {
        super(message);
        this.retryable = retryable;
    }

    /**
     * Creates an exception representing a transport-level failure.
     *
     * @param message application-readable error message
     * @param cause original transport exception
     * @param retryable whether the operation can be retried
     */
    public HenrikApiException(
        String message,
        Throwable cause,
        boolean retryable
    ) {
        super(message, cause);
        this.retryable = retryable;
    }
}
