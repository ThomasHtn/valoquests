package io.github.thomashtn.valorant.tracker.henrik.exception;

import java.time.Duration;
import lombok.Getter;
import org.springframework.http.HttpStatus;

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
        super(message, HttpStatus.TOO_MANY_REQUESTS, true);
        this.retryAfter = retryAfter;
    }
}
