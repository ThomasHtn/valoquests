package io.github.thomashtn.valorant.tracker.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the security filter protecting administrative routes.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.admin-api-key=test-admin-key-0123456789abcdef0"
    }
)
@AutoConfigureMockMvc
class AdminApiSecurityIntegrationTest extends PostgreSqlIntegrationTest {

    /**
     * MVC test client configured with the complete Spring Security chain.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Verifies that an administrative request without an API key is rejected.
     *
     * @throws Exception when MockMvc cannot execute the request
     */
    @Test
    void shouldRejectMissingAdminKey() throws Exception {
        mockMvc.perform(
                post("/api/admin/rankings/recalculation")
            )
            .andExpect(status().isUnauthorized());
    }

    /**
     * Verifies that an administrative request with an invalid API key
     * is rejected.
     *
     * @throws Exception when MockMvc cannot execute the request
     */
    @Test
    void shouldRejectInvalidAdminKey() throws Exception {
        mockMvc.perform(
                post("/api/admin/rankings/recalculation")
                    .header("X-Admin-Key", "invalid-admin-key")
            )
            .andExpect(status().isForbidden());
    }
}
