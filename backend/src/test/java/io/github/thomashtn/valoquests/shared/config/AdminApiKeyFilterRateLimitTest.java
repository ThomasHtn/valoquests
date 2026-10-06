package io.github.thomashtn.valoquests.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the invalid-key lockout of {@link AdminAuthRateLimiter} through the HTTP filter chain.
 *
 * <p>Uses its own low failure budget in a dedicated context so it does not disturb {@link AdminApiKeyFilterTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.admin-rate-limit.max-failures=2")
class AdminApiKeyFilterRateLimitTest {

    private static final String SYNCHRONIZATION_ENDPOINT =
        "/api/admin/players/1/synchronizations";

    @Autowired
    private MockMvc mockMvc;

    /**
     * Confirms that an address crossing the invalid-key budget gets HTTP 429, even with the correct key.
     *
     * @throws Exception when MockMvc cannot execute a request
     */
    @Test
    void locksOutAfterExceedingFailureBudget() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc
                .perform(
                    post(SYNCHRONIZATION_ENDPOINT)
                        .header(AdminApiKeyFilter.HEADER_NAME, "invalid-admin-key")
                )
                .andExpect(status().isForbidden());
        }

        mockMvc
            .perform(
                post(SYNCHRONIZATION_ENDPOINT)
                    .header(
                        AdminApiKeyFilter.HEADER_NAME,
                        "test-admin-key-0123456789abcdef0"
                    )
            )
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.title").value("Too Many Requests"))
            .andExpect(jsonPath("$.status").value(429))
            .andExpect(jsonPath("$.code").value("ADMIN_KEY_RATE_LIMITED"));
    }
}
