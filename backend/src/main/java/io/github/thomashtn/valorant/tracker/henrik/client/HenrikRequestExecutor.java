package io.github.thomashtn.valorant.tracker.henrik.client;

import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikApiException;
import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikRequestTimeoutException;
import io.netty.handler.timeout.TimeoutException;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

/**
 * Executes Henrik HTTP operations using the shared retry and rate-limit
 * strategies.
 *
 * <p>A connect or read timeout, a connection reset or any other transport-level failure never reaches
 * {@link HenrikResponseHandler}: that component only runs for an HTTP response Henrik actually sent.
 * {@code WebClient} instead wraps every one of these into a {@link WebClientRequestException}, whose
 * cause is the actual transport exception (a raw {@link java.io.IOException}, a
 * {@link java.util.concurrent.TimeoutException} or Netty's own {@link TimeoutException}, a distinct,
 * unrelated class despite the identical name). Left unwrapped, {@link HenrikRetryStrategy} would treat
 * every one of them as non-retryable, since it only recognizes {@link HenrikApiException}, and a
 * single dropped packet would abort an otherwise healthy match-history walk. Wrapping them here,
 * before the retry policy is applied, is what makes them retried exactly like an HTTP 503.
 */
@Component
public class HenrikRequestExecutor {

    /**
     * Shared retry-strategy factory.
     */
    private final HenrikRetryStrategy retryStrategy;

    /**
     * Global API-key request limiter.
     */
    private final HenrikRequestLimiter requestLimiter;

    /**
     * Creates the Henrik request executor.
     *
     * @param retryStrategy  retry strategy used for temporary failures
     * @param requestLimiter global Henrik API rate limiter
     */
    public HenrikRequestExecutor(
        HenrikRetryStrategy retryStrategy,
        HenrikRequestLimiter requestLimiter
    ) {
        this.retryStrategy = retryStrategy;
        this.requestLimiter = requestLimiter;
    }

    /**
     * Executes one Henrik HTTP operation.
     *
     * <p>The supplier must create the reactive HTTP request. Wrapping its
     * invocation in {@link Mono#defer(Supplier)} ensures that every physical
     * request, including a retry, acquires a rate-limit permit.</p>
     *
     * @param operationName   operation name used in retry logs
     * @param requestSupplier supplier creating the HTTP request
     * @param <T>             expected response type
     * @return Henrik response
     */
    public <T> T execute(
        String operationName,
        Supplier<Mono<T>> requestSupplier
    ) {
        Objects.requireNonNull(
            operationName,
            "operationName must not be null"
        );
        Objects.requireNonNull(
            requestSupplier,
            "requestSupplier must not be null"
        );

        return Mono.defer(() -> {
            requestLimiter.acquire();
            return requestSupplier.get();
        })
            .onErrorMap(
                WebClientRequestException.class,
                failure -> toTransportException(operationName, failure)
            )
            .retryWhen(retryStrategy.create(operationName))
            .block();
    }

    /**
     * Wraps a connector-level failure into a retryable Henrik exception.
     *
     * @param operationName operation description used in the wrapped message
     * @param failure       connector-level failure raised by {@code WebClient}
     * @return retryable Henrik exception carrying the original cause
     */
    private static HenrikApiException toTransportException(
        String operationName,
        WebClientRequestException failure
    ) {
        String message = "Henrik API operation '" + operationName
            + "' failed at the transport level: " + failure.getMessage();

        return isTimeout(failure.getCause())
            ? new HenrikRequestTimeoutException(message, failure)
            : new HenrikApiException(message, failure, true);
    }

    private static boolean isTimeout(Throwable cause) {
        return cause instanceof java.util.concurrent.TimeoutException
            || cause instanceof TimeoutException;
    }
}
