package io.github.thomashtn.valorant.tracker.henrik.client;

import io.github.thomashtn.valorant.tracker.henrik.config.HenrikApiProperties;
import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.util.retry.Retry;

/**
 * Creates retry policies for Henrik API operations.
 *
 * <p>Only temporary external failures are retried. Functional failures such as
 * an unknown player or an invalid request fail immediately.</p>
 */
@Component
public class HenrikRetryStrategy {

    /**
     * Logger used to report external retry attempts.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(HenrikRetryStrategy.class);

    /**
     * Henrik API configuration defining retry attempts and delay.
     */
    private final HenrikApiProperties properties;

    /**
     * Creates the shared Henrik retry strategy factory.
     *
     * @param properties validated Henrik API configuration
     */
    public HenrikRetryStrategy(HenrikApiProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates a retry policy for one Henrik API operation.
     *
     * <p>The initial request is not considered a retry. Therefore, a configured
     * value of three attempts results in at most two retries.</p>
     *
     * @param operationName operation description used in logs
     * @return configured Reactor retry policy
     */
    public Retry create(String operationName) {
        long maximumRetries = properties.maxAttempts() - 1L;

        return Retry.fixedDelay(
                maximumRetries,
                properties.retryDelay()
            )
            .filter(this::isRetryable)
            .doBeforeRetry(retrySignal -> LOGGER.warn(
                "Retrying Henrik API operation '{}'. Attempt {}/{}. Cause: {}",
                operationName,
                retrySignal.totalRetries() + 2,
                properties.maxAttempts(),
                retrySignal.failure().getMessage()
            ))
            .onRetryExhaustedThrow(
                (retrySpec, retrySignal) -> retrySignal.failure()
            );
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
