package io.github.thomashtn.valorant.tracker.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the invalid-key lockout enforced by {@link AdminAuthRateLimiter} through the HTTP filter
 * chain. Runs against its own low failure budget, in a dedicated Spring context, so it does not
 * interfere with the shared-context assertions in {@link AdminApiKeyFilterTest}.
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
     * Confirms that a remote address is locked out with HTTP 429 once it crosses the configured
     * invalid-key failure budget, even when it then supplies the correct key.
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
            .andExpect(status().isTooManyRequests());
    }
}
