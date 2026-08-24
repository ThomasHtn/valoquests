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
        // Reproduces the documentation wiring of the real application.properties, which
        // src/test/resources/application.properties shadows entirely. Without the path, springdoc
        // answers on its own default (/v3/api-docs) and the assertion below would be checking a
        // route production never serves.
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

    /**
     * Verifies that the API documentation is unreachable while {@code app.api-docs-enabled} is off,
     * which is the default this application ships with.
     *
     * <p>The document names every administrative route, its request body and its error codes. It
     * never leaks the key, but it hands over the map, so it must not be readable on a deployment
     * anyone can reach. Asserting it here rather than trusting the property means a future rule
     * re-opening these paths fails the build instead of shipping quietly.</p>
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
                // Any 4xx is correct — springdoc unregisters the handler (404) and the chain denies
                // the path (403) — but a success or a redirect towards the UI is not.
                if (status < 400) {
                    throw new AssertionError(
                        "API documentation answered " + status + " at " + path
                            + " while it is disabled"
                    );
                }
            });
    }
}
