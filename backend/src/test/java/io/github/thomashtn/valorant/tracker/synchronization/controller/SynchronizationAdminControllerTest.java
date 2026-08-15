package io.github.thomashtn.valorant.tracker.synchronization.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thomashtn.valorant.tracker.shared.config.AdminApiKeyFilter;
import io.github.thomashtn.valorant.tracker.shared.exception.ConflictException;
import io.github.thomashtn.valorant.tracker.synchronization.service.SynchronizationLaunchService;
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
    private SynchronizationLaunchService synchronizationLaunchService;

    /**
     * Verifies that the batch route accepts the request and hands it to the launch service.
     *
     * <p>202 rather than 200: the run outlives the request, so the response can only acknowledge
     * that it started.
     */
    @Test
    void shouldAcceptASynchronizationOfEveryPlayer() throws Exception {
        mockMvc.perform(
                post("/api/admin/synchronizations")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
            )
            .andExpect(status().isAccepted());

        verify(synchronizationLaunchService).launchAllPlayers();
    }

    /**
     * Verifies that the single-player route delegates with its path identifier.
     */
    @Test
    void shouldAcceptASynchronizationOfOnePlayer() throws Exception {
        mockMvc.perform(
                post("/api/admin/players/3/synchronizations")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
            )
            .andExpect(status().isAccepted());

        verify(synchronizationLaunchService).launchPlayer(3L);
    }

    /**
     * Verifies that a concurrent request is refused rather than queued.
     */
    @Test
    void shouldRefuseASecondConcurrentSynchronization() throws Exception {
        doThrow(new ConflictException("A synchronization is already in progress."))
            .when(synchronizationLaunchService).launchAllPlayers();

        mockMvc.perform(
                post("/api/admin/synchronizations")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    /**
     * Verifies that the deep synchronization routes are gone.
     *
     * <p>They duplicated the single flow with different season and stop rules. Asserting their
     * absence makes the removal part of the API contract rather than an implementation detail.
     *
     * <p>The batch path answers 405 rather than 404: {@code /synchronizations/deep} now matches the
     * synchronization-details route, which serves GET only. Either way the launch service is never
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

        verifyNoInteractions(synchronizationLaunchService);
    }
}
