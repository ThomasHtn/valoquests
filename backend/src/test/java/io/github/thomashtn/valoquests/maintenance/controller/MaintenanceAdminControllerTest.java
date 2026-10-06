package io.github.thomashtn.valoquests.maintenance.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thomashtn.valoquests.maintenance.service.CampaignResetService;
import io.github.thomashtn.valoquests.shared.concurrency.MatchHistoryLock;
import io.github.thomashtn.valoquests.shared.config.AdminApiKeyFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link MaintenanceAdminController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MaintenanceAdminControllerTest {

    /**
     * Administrative key configured for the test context.
     */
    private static final String ADMIN_KEY = "test-admin-key-0123456789abcdef0";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CampaignResetService campaignResetService;

    @Autowired
    private MatchHistoryLock matchHistoryLock;

    /**
     * Verifies that the reset route delegates to the service.
     */
    @Test
    void shouldResetTheCampaign() throws Exception {
        mockMvc.perform(
                post("/api/admin/maintenance/campaign-reset")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
            )
            .andExpect(status().isNoContent());

        verify(campaignResetService).resetCampaign();
    }

    /**
     * Verifies that a reset requested mid-synchronization is refused without touching anything.
     */
    @Test
    @DisplayName("Refuses the reset with a 409 while another guarded job holds the lock")
    void shouldRefuseTheResetWhileAnotherJobRuns() throws Exception {
        matchHistoryLock.acquireOrReject();
        try {
            mockMvc.perform(
                    post("/api/admin/maintenance/campaign-reset")
                        .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        } finally {
            matchHistoryLock.release();
        }

        verify(campaignResetService, never()).resetCampaign();
    }

    /**
     * Verifies that the destructive route is behind the administrator key like every other one.
     */
    @Test
    void shouldRefuseAResetWithoutAnAdminKey() throws Exception {
        mockMvc.perform(post("/api/admin/maintenance/campaign-reset"))
            .andExpect(status().isUnauthorized());
    }
}
