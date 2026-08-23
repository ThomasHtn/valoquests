package io.github.thomashtn.valoquests.henrik.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.thomashtn.valoquests.henrik.config.HenrikApiProperties;
import io.github.thomashtn.valoquests.henrik.config.HenrikClientConfig;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valoquests.henrik.exception.HenrikRateLimitException;
import io.github.thomashtn.valoquests.henrik.exception.HenrikRequestTimeoutException;
import io.github.thomashtn.valoquests.henrik.exception.HenrikServiceUnavailableException;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * HTTP tests for {@link DefaultHenrikMatchClient}.
 *
 * <p>These exercise the real request against a local server, so the query string, the page-size
 * bounds and the retry behaviour are checked as they are actually sent. The bounds matter: Henrik
 * caps a match-history page at ten, and a request outside that range fails upstream in a way the
 * walker cannot distinguish from an exhausted history.
 */
@DisplayName("Henrik match client")
class DefaultHenrikMatchClientTest {

    /**
     * PUUID used by every test.
     */
    private static final String PUUID = "a1b2c3d4-0000-0000-0000-abcdefabcdef";

    /**
     * Local HTTP server replacing HenrikDev during tests.
     */
    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("sends the configured region, platform and pagination on the request")
    void shouldSendRegionPlatformAndPagination() throws InterruptedException {
        mockWebServer.enqueue(jsonResponse(200, """
            {
              "status": 200,
              "data": [
                {
                  "metadata": {
                    "match_id": "match-1",
                    "started_at": "2026-07-15T20:00:00Z",
                    "is_completed": true,
                    "queue": { "id": "competitive", "mode_type": "Standard" },
                    "season": { "id": "e11a4", "short": "V26A4" }
                  },
                  "players": [],
                  "teams": []
                }
              ]
            }
            """));

        HenrikMatchHistoryResponse response = createClient(1).getMatches(PUUID, 20, 10);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().metadata().matchId()).isEqualTo("match-1");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath())
            .isEqualTo("/valorant/v4/by-puuid/matches/eu/pc/" + PUUID + "?start=20&size=10");
    }

    @Test
    @DisplayName("accepts an empty page, which is how a history ends")
    void shouldAcceptAnEmptyPage() {
        mockWebServer.enqueue(jsonResponse(200, """
            { "status": 200, "data": [] }
            """));

        assertThat(createClient(1).getMatches(PUUID, 600, 10).data()).isEmpty();
    }

    @Test
    @DisplayName("rejects a blank PUUID before sending anything")
    void shouldRejectABlankPuuidBeforeSendingAnything() {
        DefaultHenrikMatchClient client = createClient(1);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> client.getMatches("  ", 0, 10))
            .withMessage("puuid must not be blank");

        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("rejects a negative start before sending anything")
    void shouldRejectANegativeStartBeforeSendingAnything() {
        DefaultHenrikMatchClient client = createClient(1);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> client.getMatches(PUUID, -1, 10))
            .withMessage("start must be greater than or equal to zero");

        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("rejects a page size outside the range Henrik accepts")
    void shouldRejectAPageSizeOutsideTheSupportedRange() {
        DefaultHenrikMatchClient client = createClient(1);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> client.getMatches(PUUID, 0, 0))
            .withMessage("size must be between 1 and 10");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> client.getMatches(PUUID, 0, 11))
            .withMessage("size must be between 1 and 10");

        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("accepts both ends of the supported page-size range")
    void shouldAcceptBothEndsOfTheSupportedPageSizeRange() {
        mockWebServer.enqueue(jsonResponse(200, """
            { "status": 200, "data": [] }
            """));
        mockWebServer.enqueue(jsonResponse(200, """
            { "status": 200, "data": [] }
            """));

        DefaultHenrikMatchClient client = createClient(1);

        assertThat(client.getMatches(PUUID, 0, 1).data()).isEmpty();
        assertThat(client.getMatches(PUUID, 0, 10).data()).isEmpty();
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("retries a rate-limited page and surfaces the requested wait")
    void shouldRetryARateLimitedPage() {
        mockWebServer.enqueue(rateLimitResponse());
        mockWebServer.enqueue(rateLimitResponse());

        DefaultHenrikMatchClient client = createClient(2);

        assertThatThrownBy(() -> client.getMatches(PUUID, 0, 10))
            .isInstanceOfSatisfying(
                HenrikRateLimitException.class,
                exception -> assertThat(exception.getRetryAfter())
                    .isEqualTo(Duration.ofSeconds(1))
            );

        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("returns the page when a transient upstream failure is followed by a success")
    void shouldReturnThePageAfterATransientUpstreamFailure() {
        mockWebServer.enqueue(jsonResponse(503, """
            { "status": 503, "message": "Service temporarily unavailable" }
            """));
        mockWebServer.enqueue(jsonResponse(200, """
            { "status": 200, "data": [] }
            """));

        assertThat(createClient(2).getMatches(PUUID, 0, 10).data()).isEmpty();
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("retries a rate-limited page beyond the generic failure attempt budget")
    void shouldRetryRateLimitedPageBeyondTheGenericAttemptBudget() {
        mockWebServer.enqueue(rateLimitResponse());
        mockWebServer.enqueue(rateLimitResponse());
        mockWebServer.enqueue(jsonResponse(200, """
            { "status": 200, "data": [] }
            """));

        // A single attempt would abort on the first genuine failure, but a rate-limit response is
        // expected during a long walk and gets its own, larger budget.
        DefaultHenrikMatchClient client = createClient(1, 3);

        assertThat(client.getMatches(PUUID, 0, 10).data()).isEmpty();
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("retries a read timeout as a transient failure and eventually succeeds")
    void shouldRetryAReadTimeoutAndSucceed() {
        mockWebServer.enqueue(
            new MockResponse()
                .setHeadersDelay(500, TimeUnit.MILLISECONDS)
                .setBody("{ \"status\": 200, \"data\": [] }")
        );
        mockWebServer.enqueue(jsonResponse(200, """
            { "status": 200, "data": [] }
            """));

        DefaultHenrikMatchClient client = createClient(2, 2, Duration.ofMillis(100));

        assertThat(client.getMatches(PUUID, 0, 10).data()).isEmpty();
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("gives up after repeated read timeouts")
    void shouldGiveUpAfterRepeatedReadTimeouts() {
        mockWebServer.enqueue(
            new MockResponse().setHeadersDelay(500, TimeUnit.MILLISECONDS).setBody("{}")
        );
        mockWebServer.enqueue(
            new MockResponse().setHeadersDelay(500, TimeUnit.MILLISECONDS).setBody("{}")
        );

        DefaultHenrikMatchClient client = createClient(2, 2, Duration.ofMillis(100));

        assertThatThrownBy(() -> client.getMatches(PUUID, 0, 10))
            .isInstanceOf(HenrikRequestTimeoutException.class);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("gives up after the configured number of attempts")
    void shouldGiveUpAfterTheConfiguredNumberOfAttempts() {
        mockWebServer.enqueue(jsonResponse(503, """
            { "status": 503, "message": "Service temporarily unavailable" }
            """));
        mockWebServer.enqueue(jsonResponse(503, """
            { "status": 503, "message": "Service temporarily unavailable" }
            """));

        DefaultHenrikMatchClient client = createClient(2);

        assertThatThrownBy(() -> client.getMatches(PUUID, 0, 10))
            .isInstanceOf(HenrikServiceUnavailableException.class);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    /**
     * Creates a match client targeting the local server, using the same attempt budget for genuine
     * failures and rate-limit responses.
     *
     * @param maxAttempts maximum request attempts, including the first
     * @return the client under test
     */
    private DefaultHenrikMatchClient createClient(int maxAttempts) {
        return createClient(maxAttempts, maxAttempts);
    }

    /**
     * Creates a match client targeting the local server.
     *
     * @param maxAttempts          maximum request attempts for a genuine failure, including the first
     * @param rateLimitMaxAttempts maximum request attempts for a rate-limit response, including the
     *                             first
     * @return the client under test
     */
    private DefaultHenrikMatchClient createClient(int maxAttempts, int rateLimitMaxAttempts) {
        return createClient(maxAttempts, rateLimitMaxAttempts, Duration.ofSeconds(2));
    }

    /**
     * Creates a match client targeting the local server with a configurable read timeout.
     *
     * @param maxAttempts          maximum request attempts for a genuine failure, including the first
     * @param rateLimitMaxAttempts maximum request attempts for a rate-limit response, including the
     *                             first
     * @param readTimeout          maximum duration allowed for a response
     * @return the client under test
     */
    private DefaultHenrikMatchClient createClient(
        int maxAttempts,
        int rateLimitMaxAttempts,
        Duration readTimeout
    ) {
        HenrikApiProperties properties = new HenrikApiProperties(
            mockWebServer.url("/").toString(),
            "test-api-key",
            "eu",
            "pc",
            Duration.ofSeconds(2),
            readTimeout,
            maxAttempts,
            Duration.ofMillis(1),
            rateLimitMaxAttempts,
            30,
            Duration.ZERO
        );

        return new DefaultHenrikMatchClient(
            new HenrikClientConfig().henrikWebClient(properties),
            properties,
            new HenrikResponseHandler(),
            new HenrikRequestExecutor(
                new HenrikRetryStrategy(properties),
                new HenrikRequestLimiter(properties)
            )
        );
    }

    private MockResponse jsonResponse(int status, String body) {
        return new MockResponse()
            .setResponseCode(status)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(body);
    }

    private MockResponse rateLimitResponse() {
        return new MockResponse()
            .setResponseCode(429)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setHeader("Retry-After", "1")
            .setHeader("x-ratelimit-remaining", "0")
            .setBody("""
                { "status": 429, "message": "Rate limit exceeded" }
                """);
    }
}
