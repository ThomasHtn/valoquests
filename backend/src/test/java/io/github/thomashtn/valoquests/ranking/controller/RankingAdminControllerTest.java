package io.github.thomashtn.valoquests.ranking.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thomashtn.valoquests.ranking.service.RankingRecalculationService;
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
 * Web-layer tests for {@link RankingAdminController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RankingAdminControllerTest {

    /**
     * Administrative key configured for the test context.
     */
    private static final String ADMIN_KEY = "test-admin-key-0123456789abcdef0";

    /**
     * Route rebuilding the current weekly ranking.
     */
    private static final String RECALCULATION = "/api/admin/rankings/recalculation";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RankingRecalculationService rankingRecalculationService;

    @Autowired
    private MatchHistoryLock matchHistoryLock;

    @Test
    @DisplayName("Rebuilds the current weekly ranking and answers without a body")
    void shouldRecalculateTheCurrentRanking() throws Exception {
        mockMvc.perform(post(RECALCULATION).header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY))
            .andExpect(status().isNoContent());

        verify(rankingRecalculationService).recalculateCurrentRanking();
    }

    @Test
    @DisplayName("Refuses the recalculation with a 409 while another guarded job holds the lock")
    void shouldRefuseTheRecalculationWhileAnotherJobRuns() throws Exception {
        matchHistoryLock.acquireOrReject();
        try {
            mockMvc.perform(post(RECALCULATION).header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        } finally {
            matchHistoryLock.release();
        }

        verifyNoInteractions(rankingRecalculationService);
    }
}
