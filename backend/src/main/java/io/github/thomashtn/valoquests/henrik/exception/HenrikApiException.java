package io.github.thomashtn.valoquests.henrik.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

/**
 * Base exception for errors occurring while communicating with HenrikDev.
 */
@Getter
public class HenrikApiException extends RuntimeException {

    /**
     * HTTP status returned by Henrik, or {@code null} for transport failures.
     */
    private final HttpStatusCode statusCode;

    /**
     * Indicates whether another attempt may reasonably succeed.
     */
    private final boolean retryable;

    /**
     * Creates an exception representing an HTTP response returned by Henrik.
     *
     * @param message application-readable error message
     * @param statusCode external HTTP status
     * @param retryable whether the operation can be retried
     */
    public HenrikApiException(
        String message,
        HttpStatusCode statusCode,
        boolean retryable
    ) {
        super(message);
        this.statusCode = statusCode;
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
        this.statusCode = null;
        this.retryable = retryable;
    }
}
