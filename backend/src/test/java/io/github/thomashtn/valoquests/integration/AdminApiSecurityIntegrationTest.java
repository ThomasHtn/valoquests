package io.github.thomashtn.valoquests.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
        "app.admin-api-key=test-admin-key-0123456789abcdef0",
        // Mirrors the springdoc path of the main application.properties, which the test one shadows.
        "springdoc.api-docs.path=/api-docs",
        "springdoc.api-docs.enabled=${app.api-docs-enabled}",
        "springdoc.swagger-ui.enabled=${app.api-docs-enabled}"
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
     * Verifies that an administrative request with an invalid API key is rejected.
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
     * Verifies that percent-encoding the {@code /api/admin} prefix does not bypass the key check.
     *
     * <p>Spring MVC matches the decoded path, so a guard on the raw URI would let {@code /api/%61dmin/...} through.
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

    /**
     * Verifies that the API documentation is unreachable while {@code app.api-docs-enabled} is off, the default.
     *
     * <p>The document maps every administrative route, so a rule re-opening these paths must fail the build.
     *
     * @param path documentation route expected to stay closed
     * @throws Exception when MockMvc cannot execute the request
     */
    @ParameterizedTest
    @ValueSource(strings = {"/api-docs", "/swagger-ui.html", "/swagger-ui/index.html"})
    void shouldNotServeApiDocumentationWhenDisabled(String path) throws Exception {
        mockMvc.perform(get(path))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                // Any 4xx is fine (404 or 403), but never a success or a redirect to the UI.
                if (status < 400) {
                    throw new AssertionError(
                        "API documentation answered " + status + " at " + path
                            + " while it is disabled"
                    );
                }
            });
    }
}
