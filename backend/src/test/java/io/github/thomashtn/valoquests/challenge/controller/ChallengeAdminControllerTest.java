package io.github.thomashtn.valoquests.challenge.controller;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.challenge.service.WeeklyChallengeDrawService;
import io.github.thomashtn.valoquests.shared.concurrency.MatchHistoryLock;
import io.github.thomashtn.valoquests.shared.config.AdminApiKeyFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link ChallengeAdminController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChallengeAdminControllerTest {

    /**
     * Administrative key configured for the test context.
     */
    private static final String ADMIN_KEY = "test-admin-key-0123456789abcdef0";

    /**
     * Route redrawing the current week's pack.
     */
    private static final String REDRAW = "/api/admin/challenges/current/redraw";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeeklyChallengeDrawService weeklyDrawService;

    @MockitoBean
    private ChallengeRecalculationService recalculationService;

    @Autowired
    private MatchHistoryLock matchHistoryLock;

    @Test
    @DisplayName("Redraws the current week's pack, then rebuilds its progress")
    void shouldRedrawThenRecalculate() throws Exception {
        mockMvc.perform(post(REDRAW).header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY))
            .andExpect(status().isNoContent());

        InOrder ordered = inOrder(weeklyDrawService, recalculationService);
        ordered.verify(weeklyDrawService).redrawCurrentWeekChallenges();
        ordered.verify(recalculationService).drawAndRecalculateCurrentWeek();
    }

    @Test
    @DisplayName("Refuses the redraw with a 409 while another guarded job holds the lock")
    void shouldRefuseTheRedrawWhileAnotherJobRuns() throws Exception {
        matchHistoryLock.acquireOrReject();
        try {
            mockMvc.perform(post(REDRAW).header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        } finally {
            matchHistoryLock.release();
        }

        verifyNoInteractions(weeklyDrawService, recalculationService);
    }
}
