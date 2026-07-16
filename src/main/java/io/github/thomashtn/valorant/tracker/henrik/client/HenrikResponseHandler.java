package io.github.thomashtn.valorant.tracker.henrik.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.thomashtn.valorant.tracker.henrik.dto.HenrikErrorResponse;
import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikApiException;
import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikClientRequestException;
import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikRateLimitException;
import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikRequestTimeoutException;
import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikResourceNotFoundException;
import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikServiceUnavailableException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

/**
 * Converts unsuccessful Henrik HTTP responses into application-specific
 * exceptions.
 */
@Component
public class HenrikResponseHandler {

    /**
     * Henrik header containing the number of requests still available.
     */
    private static final String RATE_LIMIT_REMAINING_HEADER =
        "X-RateLimit-Remaining";

    /**
     * Standard HTTP header indicating how long the client should wait before
     * retrying a request.
     *
     * <p>A local constant is used because the Spring version used by the
     * project does not expose a dedicated {@link HttpHeaders} constant.</p>
     */
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    /**
     * Thread-safe JSON mapper dedicated to external Henrik error payloads.
     *
     * <p>The mapper is created locally because this component does not require
     * the complete application-wide Jackson configuration.</p>
     */
    private static final ObjectMapper ERROR_OBJECT_MAPPER =
        JsonMapper.builder()
            .findAndAddModules()
            .build();

    /**
     * Converts an unsuccessful Henrik response into a typed exception.
     *
     * @param response unsuccessful HTTP response
     * @return publisher containing the exception expected by WebClient
     */
    public Mono<Throwable> toException(ClientResponse response) {
        return response.bodyToMono(byte[].class)
            .defaultIfEmpty(new byte[0])
            .map(responseBody -> buildException(
                response.statusCode(),
                response.headers().asHttpHeaders(),
                responseBody
            ))
            .cast(Throwable.class);
    }

    /**
     * Creates the appropriate application exception for an HTTP status.
     *
     * @param statusCode external HTTP status
     * @param headers external HTTP response headers
     * @param responseBody raw external response body
     * @return typed Henrik exception
     */
    private HenrikApiException buildException(
        HttpStatusCode statusCode,
        HttpHeaders headers,
        byte[] responseBody
    ) {
        String externalMessage = readExternalMessage(responseBody)
            .orElseGet(() ->
                "Henrik API request failed with HTTP " + statusCode.value()
            );

        return switch (statusCode.value()) {
            case 404 -> new HenrikResourceNotFoundException(externalMessage);
            case 408 -> new HenrikRequestTimeoutException(externalMessage);
            case 429 -> new HenrikRateLimitException(
                buildRateLimitMessage(externalMessage, headers),
                readRetryAfter(headers).orElse(null)
            );
            case 500, 502, 503, 504 ->
                new HenrikServiceUnavailableException(externalMessage);
            default -> buildDefaultException(statusCode, externalMessage);
        };
    }

    /**
     * Creates the fallback exception for an HTTP status that does not require a
     * dedicated exception type.
     *
     * @param statusCode external HTTP status
     * @param message external error description
     * @return client or server Henrik exception
     */
    private HenrikApiException buildDefaultException(
        HttpStatusCode statusCode,
        String message
    ) {
        if (statusCode.is4xxClientError()) {
            return new HenrikClientRequestException(message, statusCode);
        }

        return new HenrikApiException(
            message,
            statusCode,
            statusCode.is5xxServerError()
        );
    }

    /**
     * Extracts the external error message from a JSON, plain-text or HTML body.
     *
     * @param responseBody raw external response body
     * @return external message when one can be extracted
     */
    private Optional<String> readExternalMessage(byte[] responseBody) {
        if (responseBody.length == 0) {
            return Optional.empty();
        }

        Optional<String> jsonMessage = readJsonMessage(responseBody);

        if (jsonMessage.isPresent()) {
            return jsonMessage;
        }

        String rawBody = new String(
            responseBody,
            StandardCharsets.UTF_8
        ).trim();

        return rawBody.isBlank()
            ? Optional.empty()
            : Optional.of(rawBody);
    }

    /**
     * Attempts to deserialize a standard Henrik JSON error body.
     *
     * @param responseBody raw external response body
     * @return deserialized external message when available
     */
    private Optional<String> readJsonMessage(byte[] responseBody) {
        try {
            HenrikErrorResponse errorResponse =
                ERROR_OBJECT_MAPPER.readValue(
                    responseBody,
                    HenrikErrorResponse.class
                );

            String message = errorResponse.message();

            return message == null || message.isBlank()
                ? Optional.empty()
                : Optional.of(message);
        } catch (IOException _) {
            /*
             * Some reverse-proxy and upstream failures return plain text or
             * HTML instead of the documented JSON structure.
             */
            return Optional.empty();
        }
    }

    /**
     * Enriches a rate-limit message with the remaining request count.
     *
     * @param message original external error description
     * @param headers external HTTP response headers
     * @return enriched rate-limit error message
     */
    private String buildRateLimitMessage(
        String message,
        HttpHeaders headers
    ) {
        String remainingRequests =
            headers.getFirst(RATE_LIMIT_REMAINING_HEADER);

        if (remainingRequests == null || remainingRequests.isBlank()) {
            return message;
        }

        return message
            + " (remaining requests: "
            + remainingRequests
            + ")";
    }

    /**
     * Reads a numeric {@code Retry-After} header expressed in seconds.
     *
     * <p>HTTP date values are intentionally ignored for the first version
     * because Henrik normally exposes the delay as a number of seconds.</p>
     *
     * @param headers external HTTP response headers
     * @return waiting duration when a valid numeric header is present
     */
    private Optional<Duration> readRetryAfter(HttpHeaders headers) {
        String retryAfterValue = headers.getFirst(RETRY_AFTER_HEADER);

        if (retryAfterValue == null || retryAfterValue.isBlank()) {
            return Optional.empty();
        }

        try {
            long seconds = Long.parseLong(retryAfterValue);

            return Optional.of(
                Duration.ofSeconds(Math.max(0L, seconds))
            );
        } catch (NumberFormatException _) {
            return Optional.empty();
        }
    }
}
