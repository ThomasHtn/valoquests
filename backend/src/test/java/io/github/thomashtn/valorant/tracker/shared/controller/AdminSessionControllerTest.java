package io.github.thomashtn.valorant.tracker.shared.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thomashtn.valorant.tracker.shared.config.AdminApiKeyFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link AdminSessionController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminSessionControllerTest {

    /**
     * Administrative key configured for the test context.
     */
    private static final String ADMIN_KEY = "test-admin-key-0123456789abcdef0";

    @Autowired
    private MockMvc mockMvc;

    /**
     * Verifies that a valid key is confirmed without any side effect.
     */
    @Test
    void shouldConfirmAValidAdminKey() throws Exception {
        mockMvc.perform(
                get("/api/admin/session")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
            )
            .andExpect(status().isNoContent());
    }

    /**
     * Verifies that a missing key and an invalid key stay distinguishable.
     *
     * <p>The two cases mean different things to whoever is signing in — nothing typed yet versus a
     * wrong value — and the login screen relies on telling them apart.
     */
    @Test
    void shouldDistinguishAMissingKeyFromAnInvalidOne() throws Exception {
        mockMvc.perform(get("/api/admin/session"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("ADMIN_KEY_MISSING"));

        mockMvc.perform(
                get("/api/admin/session")
                    .header(AdminApiKeyFilter.HEADER_NAME, "wrong-key")
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ADMIN_KEY_INVALID"));
    }
}
