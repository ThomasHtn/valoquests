package io.github.thomashtn.valorant.tracker.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies administrative API key protection through the HTTP filter chain.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminApiKeyFilterTest {

    private static final String SYNCHRONIZATION_ENDPOINT =
        "/api/admin/players/1/synchronizations";

    @Autowired
    private MockMvc mockMvc;

    /**
     * Confirms that a missing administrative key is rejected with HTTP 401.
     *
     * @throws Exception when MockMvc cannot execute the request
     */
    @Test
    void rejectsMissingAdminKey() throws Exception {
        mockMvc
            .perform(post(SYNCHRONIZATION_ENDPOINT))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Confirms that an invalid administrative key is rejected with HTTP 403.
     *
     * @throws Exception when MockMvc cannot execute the request
     */
    @Test
    void rejectsInvalidAdminKey() throws Exception {
        mockMvc
            .perform(
                post(SYNCHRONIZATION_ENDPOINT)
                    .header(
                        AdminApiKeyFilter.HEADER_NAME,
                        "invalid-admin-key"
                    )
            )
            .andExpect(status().isForbidden());
    }

    /**
     * Confirms that a valid key reaches the application controller.
     *
     * <p>The player does not exist in the test database, so the expected
     * application response is HTTP 404. Receiving 404 proves that the security
     * filter accepted the request.</p>
     *
     * @throws Exception when MockMvc cannot execute the request
     */
    @Test
    void acceptsValidAdminKey() throws Exception {
        mockMvc
            .perform(
                post(SYNCHRONIZATION_ENDPOINT)
                    .header(
                        AdminApiKeyFilter.HEADER_NAME,
                        "test-admin-key"
                    )
            )
            .andExpect(status().isNotFound());
    }
}
