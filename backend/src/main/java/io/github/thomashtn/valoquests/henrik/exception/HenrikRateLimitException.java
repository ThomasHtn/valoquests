package io.github.thomashtn.valoquests.henrik.exception;

import java.time.Duration;
import lombok.Getter;

/**
 * Indicates that Henrik temporarily rejected a request because its rate limit
 * was reached.
 */
@Getter
public class HenrikRateLimitException extends HenrikApiException {

    /**
     * Waiting duration requested by Henrik, when available.
     */
    private final Duration retryAfter;

    /**
     * Creates a retryable rate-limit exception.
     *
     * @param message external error description
     * @param retryAfter requested waiting duration, or {@code null}
     */
    public HenrikRateLimitException(
        String message,
        Duration retryAfter
    ) {
        super(message, true);
        this.retryAfter = retryAfter;
    }
}
