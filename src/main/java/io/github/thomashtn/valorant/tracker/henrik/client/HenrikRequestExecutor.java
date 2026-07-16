package io.github.thomashtn.valorant.tracker.henrik.client;

import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikApiException;
import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikRequestTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

/**
 * Executes Henrik HTTP operations with consistent transport-error conversion
 * and retry behavior.
 */
@Component
public class HenrikRequestExecutor {

    /**
     * Retry policy factory shared by every Henrik API operation.
     */
    private final HenrikRetryStrategy retryStrategy;

    /**
     * Creates the Henrik request executor.
     *
     * @param retryStrategy retry policy factory
     */
    public HenrikRequestExecutor(
        HenrikRetryStrategy retryStrategy
    ) {
        this.retryStrategy = retryStrategy;
    }

    /**
     * Executes a Henrik request and returns its decoded response synchronously.
     *
     * <p>The business layer remains synchronous because the application uses
     * Spring MVC and JPA. Reactor types are therefore contained within the
     * external HTTP infrastructure.</p>
     *
     * @param operationName operation description used in logs and errors
     * @param request supplier creating a new HTTP publisher for each attempt
     * @param <T> expected response type
     * @return decoded non-null Henrik response
     * @throws HenrikApiException when the external operation ultimately fails
     */
    public <T> T execute(
        String operationName,
        Supplier<Mono<T>> request
    ) {
        T result = request.get()
            .onErrorMap(
                WebClientRequestException.class,
                exception -> convertTransportFailure(
                    operationName,
                    exception
                )
            )
            .retryWhen(retryStrategy.create(operationName))
            .block();

        return Objects.requireNonNull(
            result,
            () -> "Henrik API operation returned no response: "
                + operationName
        );
    }

    /**
     * Converts a WebClient transport exception into a typed Henrik exception.
     *
     * @param operationName operation being executed
     * @param exception original WebClient transport exception
     * @return retryable Henrik exception
     */
    private HenrikApiException convertTransportFailure(
        String operationName,
        WebClientRequestException exception
    ) {
        if (containsReadTimeout(exception)) {
            return new HenrikRequestTimeoutException(
                "Henrik API operation timed out: " + operationName,
                exception
            );
        }

        return new HenrikApiException(
            "Unable to communicate with Henrik API during operation: "
                + operationName,
            exception,
            true
        );
    }

    /**
     * Searches the complete exception cause chain for a Netty read timeout.
     *
     * @param throwable exception whose causes must be inspected
     * @return {@code true} when a read timeout exists in the cause chain
     */
    private boolean containsReadTimeout(Throwable throwable) {
        Throwable currentCause = throwable;

        while (currentCause != null) {
            if (currentCause instanceof ReadTimeoutException) {
                return true;
            }

            currentCause = currentCause.getCause();
        }

        return false;
    }
}
