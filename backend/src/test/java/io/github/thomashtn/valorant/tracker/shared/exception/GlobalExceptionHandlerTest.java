package io.github.thomashtn.valorant.tracker.shared.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikApiException;
import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikRateLimitException;
import io.github.thomashtn.valorant.tracker.match.model.MatchHistoryFilter;
import io.github.thomashtn.valorant.tracker.match.service.MatchQueryService;
import io.github.thomashtn.valorant.tracker.shared.config.AdminApiKeyFilter;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies how failures are turned into HTTP responses.
 *
 * <p>The point of these tests is the boundary between "the caller got it wrong" and "we got it
 * wrong". Getting that boundary wrong is invisible in a green build: the API keeps answering, it
 * just blames the wrong party and hands internal text to whoever asked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Error responses")
class GlobalExceptionHandlerTest {

    /**
     * Path used to drive the handler through a real controller.
     */
    private static final String MATCHES = "/api/players/1/matches";

    /**
     * Administrative key configured for the test context.
     */
    private static final String ADMIN_KEY = "test-admin-key-0123456789abcdef0";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchQueryService matchQueryService;

    /**
     * Makes the query service fail with the supplied exception.
     */
    private void failWith(RuntimeException failure) {
        when(matchQueryService.findByPlayer(
            anyLong(), anyInt(), anyInt(), any(MatchHistoryFilter.class)
        )).thenThrow(failure);
    }

    @Test
    @DisplayName("answers 400 and explains what to correct for an invalid request value")
    void shouldAnswerBadRequestForAnInvalidRequestValue() throws Exception {
        failWith(new InvalidRequestException("size must be between 1 and 100"));

        mockMvc.perform(get(MATCHES))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
            .andExpect(jsonPath("$.detail").value("size must be between 1 and 100"));
    }

    @Test
    @DisplayName("answers 500 without leaking the message when an internal expectation breaks")
    void shouldNotBlameTheCallerForABrokenInternalExpectation() throws Exception {
        failWith(new IllegalArgumentException("stopReason must not be null"));

        mockMvc.perform(get(MATCHES))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
            .andExpect(content().string(not(
                containsString("stopReason")
            )));
    }

    @Test
    @DisplayName("answers 400 naming the parameter when a path value has the wrong type")
    void shouldAnswerBadRequestForAPathValueOfTheWrongType() throws Exception {
        mockMvc.perform(get("/api/players/not-a-number/matches"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
            .andExpect(jsonPath("$.detail").value("Parameter 'playerId' has an invalid value."));
    }

    @Test
    @DisplayName("answers 400 rather than 500 when a query value cannot be bound")
    void shouldAnswerBadRequestForAQueryValueThatCannotBeBound() throws Exception {
        mockMvc.perform(get(MATCHES).param("seasonId", "not-a-number"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("answers 502 without exposing the upstream failure text")
    void shouldNotExposeUpstreamFailureDetail() throws Exception {
        failWith(new HenrikApiException(
            "GET https://api.henrikdev.xyz/valorant/v4/by-puuid/matches failed with 503",
            HttpStatus.SERVICE_UNAVAILABLE,
            true
        ));

        mockMvc.perform(get(MATCHES))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.code").value("HENRIK_API_ERROR"))
            .andExpect(content().string(not(
                containsString("henrikdev.xyz")
            )));
    }

    @Test
    @DisplayName("answers 429 without exposing the upstream failure text")
    void shouldAnswerTooManyRequestsWithoutUpstreamDetail() throws Exception {
        failWith(new HenrikRateLimitException(
            "key 1a2b3c exhausted its quota",
            Duration.ofSeconds(30)
        ));

        mockMvc.perform(get(MATCHES))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("HENRIK_RATE_LIMIT_EXCEEDED"))
            .andExpect(content().string(not(
                containsString("1a2b3c")
            )));
    }

    @Test
    @DisplayName("answers 404 rather than 500 for a path this API does not expose")
    void shouldAnswerNotFoundForAnUnknownPath() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("answers 405 rather than 500 for an unsupported method on an authorized route")
    void shouldAnswerMethodNotAllowedForAnUnsupportedMethod() throws Exception {
        // A public route only permits GET, so anything else is denied by security before routing.
        // Reaching the 405 handler at all therefore requires an authorized admin route.
        mockMvc.perform(
                post("/api/admin/synchronizations/1")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
            )
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("denies an unsupported method on a public route before it reaches a controller")
    void shouldDenyAnUnsupportedMethodOnAPublicRoute() throws Exception {
        mockMvc.perform(post(MATCHES))
            .andExpect(status().isForbidden());
    }
}
