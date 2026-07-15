package io.github.thomashtn.valorant.tracker.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies administrative API key protection through the HTTP filter chain.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminApiKeyFilterTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Confirms that an administrative request without a key is rejected.
     *
     * @throws Exception when MockMvc cannot execute the request
     */
    @Test
    void rejectsMissingAdminKey() throws Exception {
        mockMvc
            .perform(post("/api/admin/synchronizations"))
            .andExpect(status().isUnauthorized());
    }
}
