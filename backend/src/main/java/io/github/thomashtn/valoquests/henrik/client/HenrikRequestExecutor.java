package io.github.thomashtn.valoquests.henrik.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.henrik.exception.HenrikApiException;
import io.github.thomashtn.valoquests.henrik.exception.HenrikRequestTimeoutException;
import io.netty.handler.timeout.TimeoutException;
import java.net.URI;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

/**
 * Sends every Henrik HTTP request, behind the shared rate limiter and retry policy.
 *
 * <p>An HTTP error response is turned into a typed exception by {@link HenrikResponseHandler}.
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
     * HTTP client configured for Henrik API calls.
     */
    private final WebClient henrikWebClient;

    /**
     * Converts Henrik HTTP failures into typed application exceptions.
     */
    private final HenrikResponseHandler responseHandler;

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
     * @param henrikWebClient configured Henrik HTTP client
     * @param responseHandler external response error handler
     * @param retryStrategy   retry strategy used for temporary failures
     * @param requestLimiter  global Henrik API rate limiter
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public HenrikRequestExecutor(
        WebClient henrikWebClient,
        HenrikResponseHandler responseHandler,
        HenrikRetryStrategy retryStrategy,
        HenrikRequestLimiter requestLimiter
    ) {
        this.henrikWebClient = henrikWebClient;
        this.responseHandler = responseHandler;
        this.retryStrategy = retryStrategy;
        this.requestLimiter = requestLimiter;
    }

    /**
     * Sends one GET request to Henrik and decodes its body.
     *
     * <p>URI variables passed to {@link UriBuilder#build(Object...)} are encoded, so a Riot ID holding
     * spaces or special characters cannot corrupt the request path.
     *
     * @param operationName operation name used in retry logs and transport errors
     * @param uri           builds the request URI relative to the Henrik base URL
     * @param responseType  type the response body is decoded into
     * @param <T>           expected response type
     * @return decoded Henrik response
     */
    public <T> T get(
        String operationName,
        Function<UriBuilder, URI> uri,
        Class<T> responseType
    ) {
        return execute(operationName, () -> henrikWebClient.get()
            .uri(uri)
            .retrieve()
            .onStatus(HttpStatusCode::isError, responseHandler::toException)
            .bodyToMono(responseType));
    }

    /**
     * Rejects a blank request argument before any request goes out.
     *
     * @param value     argument to check
     * @param fieldName name used in the validation message
     * @throws IllegalArgumentException when the value is null or blank
     */
    static void requireText(String value, String fieldName) {
        if (Objects.requireNonNullElse(value, "").isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
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
    <T> T execute(
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

    /**
     * Whether a transport failure was a timeout, whichever timeout class raised it.
     */
    private static boolean isTimeout(Throwable cause) {
        return cause instanceof java.util.concurrent.TimeoutException
            || cause instanceof TimeoutException;
    }
}
