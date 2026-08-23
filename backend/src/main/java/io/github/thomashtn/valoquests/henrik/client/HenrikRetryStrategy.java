package io.github.thomashtn.valoquests.henrik.client;

import io.github.thomashtn.valoquests.henrik.config.HenrikApiProperties;
import io.github.thomashtn.valoquests.henrik.exception.HenrikApiException;
import io.github.thomashtn.valoquests.henrik.exception.HenrikRateLimitException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Creates retry policies for Henrik API operations.
 *
 * <p>Only temporary external failures are retried. Functional failures such as
 * an unknown player or an invalid request fail immediately.</p>
 *
 * <p>Rate-limit responses use Henrik's {@code Retry-After} value when it is
 * available. Other temporary failures use the configured default delay.</p>
 */
@Component
public class HenrikRetryStrategy {

    /**
     * Logger used to report external retry attempts.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(HenrikRetryStrategy.class);

    /**
     * Henrik API configuration defining retry attempts and fallback delay.
     */
    private final HenrikApiProperties properties;

    /**
     * Creates the shared Henrik retry-strategy factory.
     *
     * @param properties validated Henrik API configuration
     */
    public HenrikRetryStrategy(HenrikApiProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates a retry policy for one Henrik API operation.
     *
     * <p>The first HTTP request is included in {@code maxAttempts}. Therefore,
     * three configured attempts allow at most two retries.</p>
     *
     * @param operationName operation description used in logs
     * @return configured Reactor retry policy
     */
    public Retry create(String operationName) {
        return Retry.from(retrySignals ->
            retrySignals.concatMap(retrySignal -> {
                Throwable failure = retrySignal.failure();

                if (!isRetryable(failure)) {
                    return Mono.error(failure);
                }

                long configuredAttempts = maximumAttempts(failure);
                if (retrySignal.totalRetries() >= configuredAttempts - 1L) {
                    return Mono.error(failure);
                }

                Duration delay = determineDelay(failure);
                long nextAttempt = retrySignal.totalRetries() + 2L;

                LOGGER.warn(
                    "Retrying Henrik API operation '{}'. "
                        + "Attempt {}/{} in {} ms. Cause: {}",
                    operationName,
                    nextAttempt,
                    configuredAttempts,
                    delay.toMillis(),
                    failure.getMessage()
                );

                return Mono.delay(delay);
            })
        );
    }

    /**
     * Determines the attempt budget for a failure.
     *
     * <p>A rate-limit response is Henrik's own advertised, expected condition during a long
     * pagination walk, not a genuine failure: it gets a far more generous budget than a real error
     * such as a timeout or an upstream outage, so a burst of 429 responses does not abort a walk that
     * would otherwise have completed.
     *
     * @param failure retryable Henrik failure
     * @return maximum number of HTTP attempts, including the first
     */
    private long maximumAttempts(Throwable failure) {
        return failure instanceof HenrikRateLimitException
            ? properties.rateLimitMaxAttempts()
            : properties.maxAttempts();
    }

    /**
     * Determines the waiting duration before retrying a request.
     *
     * <p>For a rate-limit response, the duration requested by Henrik takes
     * precedence when it is greater than the configured fallback delay.</p>
     *
     * @param failure retryable Henrik failure
     * @return waiting duration
     */
    private Duration determineDelay(Throwable failure) {
        Duration configuredDelay = properties.retryDelay();

        if (!(failure instanceof HenrikRateLimitException rateLimitException)) {
            return configuredDelay;
        }

        Duration retryAfter = rateLimitException.getRetryAfter();

        if (retryAfter == null
            || retryAfter.isNegative()
            || retryAfter.isZero()) {
            return configuredDelay;
        }

        return retryAfter.compareTo(configuredDelay) > 0
            ? retryAfter
            : configuredDelay;
    }

    /**
     * Determines whether a failure should trigger another HTTP attempt.
     *
     * @param throwable failure emitted by the HTTP operation
     * @return {@code true} only for retryable Henrik failures
     */
    private boolean isRetryable(Throwable throwable) {
        return throwable instanceof HenrikApiException exception
            && exception.isRetryable();
    }
}
