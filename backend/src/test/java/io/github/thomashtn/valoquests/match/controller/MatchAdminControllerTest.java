package io.github.thomashtn.valoquests.match.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.exception.MatchNotFoundException;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.GameModeSource;
import io.github.thomashtn.valoquests.match.service.MatchCorrectionService;
import io.github.thomashtn.valoquests.shared.config.AdminApiKeyFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link MatchAdminController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MatchAdminControllerTest {

    /**
     * Administrative key configured for the test context.
     */
    private static final String ADMIN_KEY = "test-admin-key-0123456789abcdef0";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchCorrectionService correctionService;

    /**
     * Verifies that a valid correction request delegates to the correction service and reports its
     * outcome.
     */
    @Test
    void shouldCorrectGameMode() throws Exception {
        ValorantMatch corrected = new ValorantMatch();
        corrected.setId(1204L);
        corrected.setGameMode(GameMode.DEATHMATCH);
        corrected.setGameModeSource(GameModeSource.MANUALLY_CORRECTED);

        when(correctionService.correctGameMode(1204L, GameMode.DEATHMATCH))
            .thenReturn(corrected);

        mockMvc.perform(
                patch("/api/admin/matches/1204/game-mode")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        { "gameMode": "DEATHMATCH" }
                        """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1204))
            .andExpect(jsonPath("$.gameMode").value("DEATHMATCH"))
            .andExpect(jsonPath("$.gameModeSource").value("MANUALLY_CORRECTED"));

        verify(correctionService).correctGameMode(1204L, GameMode.DEATHMATCH);
    }

    /**
     * Verifies that a missing game mode is rejected before the service is reached.
     */
    @Test
    void shouldRejectMissingGameMode() throws Exception {
        mockMvc.perform(
                patch("/api/admin/matches/1204/game-mode")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
            .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that correcting an unknown match surfaces as a 404 response.
     */
    @Test
    void shouldReportAnUnknownMatch() throws Exception {
        when(correctionService.correctGameMode(999L, GameMode.DEATHMATCH))
            .thenThrow(new MatchNotFoundException(999L));

        mockMvc.perform(
                patch("/api/admin/matches/999/game-mode")
                    .header(AdminApiKeyFilter.HEADER_NAME, ADMIN_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        { "gameMode": "DEATHMATCH" }
                        """)
            )
            .andExpect(status().isNotFound());
    }

    /**
     * Verifies that the missing administrative key is rejected before the service is reached.
     */
    @Test
    void shouldRejectRequestsWithoutTheAdminKey() throws Exception {
        mockMvc.perform(
                patch("/api/admin/matches/1204/game-mode")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        { "gameMode": "DEATHMATCH" }
                        """)
            )
            .andExpect(status().isUnauthorized());
    }
}
