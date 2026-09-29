package io.github.thomashtn.valoquests.match.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thomashtn.valoquests.match.dto.MatchResponse;
import io.github.thomashtn.valoquests.match.dto.SquadMatchResponse;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.match.service.MatchQueryService;
import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link SquadMatchController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SquadMatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchQueryService queryService;

    /**
     * Verifies that the squad history is public and names each match after its player.
     */
    @Test
    void shouldExposeTheSquadHistoryWithoutAnAdminKey() throws Exception {
        MatchResponse match = new MatchResponse(
            42L, Instant.parse("2026-07-15T20:00:00Z"), "Ascent", GameMode.COMPETITIVE, "Omen",
            MatchResult.WIN, 13, 7, 20, 10, 5, BigDecimal.valueOf(2), BigDecimal.valueOf(230),
            BigDecimal.valueOf(150), null, CompetitiveTier.UNRANKED, 500, 100, 0, 150, 350
        );
        when(queryService.findSquad(1, 20)).thenReturn(new PageResponse<>(
            List.of(new SquadMatchResponse(3L, "natank", "Sova", match)), 1, 20, 21, 2
        ));

        mockMvc.perform(get("/api/matches").param("page", "1").param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].playerId").value(3))
            .andExpect(jsonPath("$.content[0].displayName").value("natank"))
            .andExpect(jsonPath("$.content[0].match.id").value(42))
            .andExpect(jsonPath("$.totalPages").value(2));
    }
}
