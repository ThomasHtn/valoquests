package io.github.thomashtn.valorant.tracker.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
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

    /**
     * Verifies that percent-encoding a character of the {@code /api/admin} prefix does not slip
     * past the administrative key check.
     *
     * <p>Spring MVC matches handler mappings on the <em>decoded</em> path, so
     * {@code /api/%61dmin/...} reaches the very same controller as {@code /api/admin/...}. A guard
     * comparing the raw request URI would not recognise it as administrative and would let the
     * request through unauthenticated.</p>
     *
     * @throws Exception when MockMvc cannot execute the request
     */
    @Test
    void shouldRejectAdminRouteReachedThroughPercentEncodedPath() throws Exception {
        mockMvc.perform(
                post(URI.create("/api/%61dmin/rankings/recalculation"))
            )
            .andExpect(status().isUnauthorized());
    }

    /**
     * Verifies that the same encoding trick does not expose administrative read endpoints, which
     * Spring Security additionally permits through the public {@code GET /api/**} rule.
     *
     * @throws Exception when MockMvc cannot execute the request
     */
    @Test
    void shouldRejectAdminReadRouteReachedThroughPercentEncodedPath() throws Exception {
        mockMvc.perform(
                get(URI.create("/api/%61dmin/players"))
            )
            .andExpect(status().isUnauthorized());
    }
}
