package io.github.thomashtn.valoquests.player.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thomashtn.valoquests.player.dto.PlayerContributionResponse;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.service.PlayerContributionQueryService;
import io.github.thomashtn.valoquests.ranking.model.WeeklyTitle;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for the contribution route of {@link PlayerController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerContributionQueryService contributionService;

    /**
     * Verifies that the contribution block is exposed, null campaign included.
     */
    @Test
    void shouldExposeThePlayersContribution() throws Exception {
        when(contributionService.findByPlayerId(3L)).thenReturn(new PlayerContributionResponse(
            3L,
            new PlayerContributionResponse.WeekContributionResponse(
                LocalDate.of(2026, 9, 7), 1, 1_500, 450, 1_050, 6, 3, 4, 120, 2, 1, 1_620, List.of(WeeklyTitle.REGULAR)
            ),
            null
        ));

        mockMvc.perform(get("/api/players/3/contribution"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.playerId").value(3))
            .andExpect(jsonPath("$.week.totalPoints").value(1_620))
            .andExpect(jsonPath("$.week.titles[0]").value("REGULAR"))
            .andExpect(jsonPath("$.campaign").value(nullValue()));
    }

    /**
     * Verifies that an unknown player answers 404.
     */
    @Test
    void shouldAnswerNotFoundForAnUnknownPlayer() throws Exception {
        when(contributionService.findByPlayerId(99L)).thenThrow(new PlayerNotFoundException(99L));

        mockMvc.perform(get("/api/players/99/contribution"))
            .andExpect(status().isNotFound());
    }
}
