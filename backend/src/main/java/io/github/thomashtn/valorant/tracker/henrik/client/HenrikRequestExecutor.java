package io.github.thomashtn.valorant.tracker.henrik.client;

import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Executes Henrik HTTP operations using the shared retry and rate-limit
 * strategies.
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
            .retryWhen(retryStrategy.create(operationName))
            .block();
    }
}
