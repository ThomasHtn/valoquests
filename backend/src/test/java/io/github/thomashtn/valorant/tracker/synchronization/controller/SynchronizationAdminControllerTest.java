package io.github.thomashtn.valorant.tracker.synchronization.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thomashtn.valorant.tracker.shared.config.AdminApiKeyFilter;
import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationResponse;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationType;
import io.github.thomashtn.valorant.tracker.synchronization.service.SynchronizationCommandService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link SynchronizationAdminController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SynchronizationAdminControllerTest {

    /**
     * Administrative key configured for the test context.
     */
    private static final String ADMIN_KEY = "test-admin-key-0123456789abcdef0";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SynchronizationCommandService synchronizationService;

    /**
     * Verifies that the batch route delegates to the command service.
     */
    @Test
    void shouldSynchronizeEveryActivePlayer() throws Exception {
        when(synchronizationService.synchronizeAllPlayers()).thenReturn(response());

        mockMvc.perform(
                post("/api/admin/synchronizations")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.matchesImported").value(12));

        verify(synchronizationService).synchronizeAllPlayers();
    }

    /**
     * Verifies that the single-player route delegates with its path identifier.
     */
    @Test
    void shouldSynchronizeOnePlayer() throws Exception {
        when(synchronizationService.synchronizePlayer(3L)).thenReturn(response());

        mockMvc.perform(
                post("/api/admin/players/3/synchronizations")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
            )
            .andExpect(status().isOk());

        verify(synchronizationService).synchronizePlayer(3L);
    }

    /**
     * Verifies that the deep synchronization routes are gone.
     *
     * <p>They duplicated the single flow with different season and stop rules. Asserting their
     * absence makes the removal part of the API contract rather than an implementation detail.
     *
     * <p>The batch path answers 405 rather than 404: {@code /synchronizations/deep} now matches the
     * synchronization-details route, which serves GET only. Either way the command service is never
     * reached, which is what actually matters.
     */
    @Test
    void shouldNoLongerExposeDeepSynchronizationRoutes() throws Exception {
        mockMvc.perform(
                post("/api/admin/synchronizations/deep")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
            )
            .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(
                post("/api/admin/players/3/synchronizations/deep")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
            )
            .andExpect(status().isNotFound());

        verifyNoInteractions(synchronizationService);
    }

    /**
     * Creates a completed synchronization summary.
     */
    private SynchronizationResponse response() {
        Instant startedAt = Instant.parse("2026-07-25T06:00:00Z");
        return new SynchronizationResponse(
            1L,
            SynchronizationType.STANDARD,
            SynchronizationTrigger.MANUAL,
            SynchronizationStatus.COMPLETED,
            startedAt,
            startedAt.plusSeconds(90),
            startedAt,
            startedAt.plusSeconds(90),
            6,
            0,
            12,
            null
        );
    }
}
