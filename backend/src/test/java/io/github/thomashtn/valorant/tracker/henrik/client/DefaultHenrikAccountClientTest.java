package io.github.thomashtn.valorant.tracker.henrik.client;

import io.github.thomashtn.valorant.tracker.henrik.config.HenrikApiProperties;
import io.github.thomashtn.valorant.tracker.henrik.config.HenrikClientConfig;
import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikRateLimitException;
import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikResourceNotFoundException;
import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikServiceUnavailableException;
import io.github.thomashtn.valorant.tracker.henrik.mapper.HenrikAccountMapper;
import io.github.thomashtn.valorant.tracker.henrik.model.HenrikAccount;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HTTP integration tests for {@link DefaultHenrikAccountClient}.
 *
 * <p>The tests use a local HTTP server and therefore exercise the real
 * WebClient request, JSON deserialization, response handling, retry policy and
 * account mapping without contacting HenrikDev.</p>
 */
class DefaultHenrikAccountClientTest {

    /**
     * Local HTTP server replacing HenrikDev during tests.
     */
    private MockWebServer mockWebServer;

    /**
     * Starts a fresh local HTTP server before each test.
     *
     * @throws IOException when the server cannot be started
     */
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    /**
     * Stops the local HTTP server after each test.
     *
     * @throws IOException when the server cannot be stopped cleanly
     */
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    /**
     * Verifies the complete successful account resolution flow.
     *
     * @throws InterruptedException when the recorded request cannot be read
     */
    @Test
    void shouldResolveAccountFromSuccessfulResponse()
        throws InterruptedException {

        mockWebServer.enqueue(jsonResponse(200, """
            {
              "status": 200,
              "data": {
                "puuid": "resolved-puuid",
                "name": "Psilonnix",
                "tag": "EUW",
                "region": "eu",
                "account_level": 250
              }
            }
            """));

        DefaultHenrikAccountClient client = createClient(1);

        HenrikAccount account = client.getAccount(
            "Psilonnix",
            "EUW"
        );

        assertThat(account).isEqualTo(
            new HenrikAccount(
                "resolved-puuid",
                "Psilonnix",
                "EUW"
            )
        );

        RecordedRequest request = mockWebServer.takeRequest();

        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath())
            .isEqualTo("/valorant/v2/account/Psilonnix/EUW");

        assertThat(request.getHeader(HttpHeaders.AUTHORIZATION))
            .isEqualTo("test-api-key");

        assertThat(request.getHeader(HttpHeaders.ACCEPT))
            .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * Verifies that spaces contained in a Riot game name are URI encoded.
     *
     * @throws InterruptedException when the recorded request cannot be read
     */
    @Test
    void shouldEncodeRiotIdPathVariables()
        throws InterruptedException {

        mockWebServer.enqueue(jsonResponse(200, """
            {
              "status": 200,
              "data": {
                "puuid": "nata-nk-puuid",
                "name": "MDR nataNk",
                "tag": "1wnl"
              }
            }
            """));

        DefaultHenrikAccountClient client = createClient(1);

        HenrikAccount account = client.getAccount(
            "MDR nataNk",
            "1wnl"
        );

        assertThat(account.gameName())
            .isEqualTo("MDR nataNk");

        assertThat(mockWebServer.takeRequest().getPath())
            .isEqualTo(
                "/valorant/v2/account/MDR%20nataNk/1wnl"
            );
    }

    /**
     * Verifies that a missing Riot account becomes a typed non-retryable
     * exception.
     */
    @Test
    void shouldConvertNotFoundResponse() {
        mockWebServer.enqueue(jsonResponse(404, """
            {
              "status": 404,
              "message": "Account not found"
            }
            """));

        DefaultHenrikAccountClient client = createClient(3);

        assertThatThrownBy(
            () -> client.getAccount("Unknown", "EUW")
        )
            .isInstanceOf(
                HenrikResourceNotFoundException.class
            )
            .hasMessage("Account not found");

        assertThat(mockWebServer.getRequestCount())
            .isEqualTo(1);
    }

    /**
     * Verifies that a rate-limit response is retried and preserves the
     * Retry-After value when every attempt fails.
     */
    @Test
    void shouldRetryRateLimitResponse() {
        mockWebServer.enqueue(rateLimitResponse());
        mockWebServer.enqueue(rateLimitResponse());

        DefaultHenrikAccountClient client = createClient(2);

        assertThatThrownBy(
            () -> client.getAccount("Psilonnix", "EUW")
        )
            .isInstanceOfSatisfying(
                HenrikRateLimitException.class,
                exception -> {
                    assertThat(exception.getMessage())
                        .isEqualTo(
                            "Rate limit exceeded "
                                + "(remaining requests: 0)"
                        );

                    assertThat(exception.getRetryAfter())
                        .isEqualTo(Duration.ofSeconds(1));
                }
            );

        assertThat(mockWebServer.getRequestCount())
            .isEqualTo(2);
    }

    /**
     * Verifies that temporary server failures are retried before the final
     * typed exception is propagated.
     */
    @Test
    void shouldRetryServiceUnavailableResponse() {
        mockWebServer.enqueue(jsonResponse(503, """
            {
              "status": 503,
              "message": "Service temporarily unavailable"
            }
            """));

        mockWebServer.enqueue(jsonResponse(503, """
            {
              "status": 503,
              "message": "Service temporarily unavailable"
            }
            """));

        DefaultHenrikAccountClient client = createClient(2);

        assertThatThrownBy(
            () -> client.getAccount("Psilonnix", "EUW")
        )
            .isInstanceOf(
                HenrikServiceUnavailableException.class
            )
            .hasMessage(
                "Service temporarily unavailable"
            );

        assertThat(mockWebServer.getRequestCount())
            .isEqualTo(2);
    }

    /**
     * Verifies that an invalid game name is rejected before any HTTP request.
     */
    @Test
    void shouldRejectBlankGameNameBeforeSendingRequest() {
        DefaultHenrikAccountClient client = createClient(1);

        assertThatIllegalArgumentException()
            .isThrownBy(
                () -> client.getAccount(" ", "EUW")
            )
            .withMessage(
                "gameName must not be blank"
            );

        assertThat(mockWebServer.getRequestCount())
            .isZero();
    }

    /**
     * Verifies that a missing tag line is rejected before any HTTP request.
     */
    @Test
    void shouldRejectNullTagLineBeforeSendingRequest() {
        DefaultHenrikAccountClient client = createClient(1);

        assertThatIllegalArgumentException()
            .isThrownBy(
                () -> client.getAccount(
                    "Psilonnix",
                    null
                )
            )
            .withMessage(
                "tagLine must not be blank"
            );

        assertThat(mockWebServer.getRequestCount())
            .isZero();
    }

    /**
     * Creates a fully configured account client targeting the local server.
     *
     * @param maxAttempts maximum request attempts including the first request
     * @return Henrik account client under test
     */
    private DefaultHenrikAccountClient createClient(
        int maxAttempts
    ) {
        HenrikApiProperties properties =
            new HenrikApiProperties(
                mockWebServer.url("/").toString(),
                "test-api-key",
                "eu",
                "pc",
                Duration.ofSeconds(2),
                Duration.ofSeconds(2),
                maxAttempts,
                Duration.ofMillis(1),
                30,
                Duration.ZERO
            );

        WebClient webClient =
            new HenrikClientConfig()
                .henrikWebClient(properties);

        HenrikRetryStrategy retryStrategy =
            new HenrikRetryStrategy(properties);

        HenrikRequestLimiter requestLimiter =
            new HenrikRequestLimiter(properties);

        HenrikRequestExecutor requestExecutor =
            new HenrikRequestExecutor(
                retryStrategy,
                requestLimiter
            );

        return new DefaultHenrikAccountClient(
            webClient,
            new HenrikResponseHandler(),
            requestExecutor,
            new HenrikAccountMapper()
        );
    }

    /**
     * Creates a JSON HTTP response.
     *
     * @param status HTTP status
     * @param body   JSON response body
     * @return configured mock response
     */
    private MockResponse jsonResponse(
        int status,
        String body
    ) {
        return new MockResponse()
            .setResponseCode(status)
            .setHeader(
                HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_JSON_VALUE
            )
            .setBody(body);
    }

    /**
     * Creates a Henrik rate-limit response.
     *
     * @return configured mock response
     */
    private MockResponse rateLimitResponse() {
        return jsonResponse(429, """
            {
              "status": 429,
              "message": "Rate limit exceeded"
            }
            """)
            .setHeader("Retry-After", "1")
            .setHeader(
                "X-RateLimit-Remaining",
                "0"
            );
    }
}
