package io.github.thomashtn.valoquests.week.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thomashtn.valoquests.shared.concurrency.MatchHistoryLock;
import io.github.thomashtn.valoquests.shared.config.AdminApiKeyFilter;
import io.github.thomashtn.valoquests.week.service.WeeklyRolloverService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link WeekAdminController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WeekAdminControllerTest {

    /**
     * Administrative key configured for the test context.
     */
    private static final String ADMIN_KEY = "test-admin-key-0123456789abcdef0";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeeklyRolloverService weeklyRolloverService;

    @Autowired
    private MatchHistoryLock matchHistoryLock;

    /**
     * Verifies that the route runs the same catch-up rollover the Monday schedule runs.
     *
     * <p>The repair path for a rollover that never fired: without it a past week's fight stays
     * unresolved until the next Monday, and both the campaign map and the colony's morale keep
     * reading it as a week that was never fought.
     */
    @Test
    void shouldRunTheRolloverNow() throws Exception {
        mockMvc.perform(
                post("/api/admin/weeks/rollover")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
            )
            .andExpect(status().isNoContent());

        verify(weeklyRolloverService).rolloverIfNeeded();
    }

    /**
     * Verifies that an admin rollover cannot overlap a synchronization or the scheduled rollover.
     */
    @Test
    @DisplayName("Refuses the rollover with a 409 while another guarded job holds the lock")
    void shouldRefuseTheRolloverWhileAnotherJobRuns() throws Exception {
        matchHistoryLock.acquireOrReject();
        try {
            mockMvc.perform(
                    post("/api/admin/weeks/rollover")
                        .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        } finally {
            matchHistoryLock.release();
        }

        verify(weeklyRolloverService, never()).rolloverIfNeeded();
    }
}
