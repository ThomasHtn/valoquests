package io.github.thomashtn.valoquests.ranking.controller;

import io.github.thomashtn.valoquests.ranking.dto.CurrentRankingResponse;
import io.github.thomashtn.valoquests.ranking.dto.RankingHistoryWeekResponse;
import io.github.thomashtn.valoquests.ranking.service.RankingQueryService;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the active ranking and finalized weekly ranking history.
 */
@RestController
@RequestMapping(value = "/api/rankings", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Rankings", description = "Current weekly standings and finalized historical standings.")
public class RankingController {

    /**
     * Service used to query current and historical rankings.
     */
    private final RankingQueryService rankingQueryService;

    /**
     * @param rankingQueryService ranking query service
     */
    public RankingController(RankingQueryService rankingQueryService) {
        this.rankingQueryService = rankingQueryService;
    }

    /**
     * @return active-week ranking with exact progress for every player and challenge
     */
    @GetMapping("/current")
    @Operation(
        summary = "Get the current weekly ranking",
        description = """
            Returns each player's current position, score, completed-challenge count and exact
            progress toward every challenge selected for the active calendar week.
            """
    )
    @ApiResponse(responseCode = "200", description = "Current ranking returned successfully.")
    public CurrentRankingResponse getCurrentRanking() {
        return rankingQueryService.findCurrent();
    }

    /**
     * @return one page of finalized weekly rankings
     *
     * @param page zero-based page index
     * @param size number of finalized weeks returned per page
     */
    @GetMapping("/history")
    @Operation(
        summary = "Get finalized weekly rankings",
        description = """
            Returns a page of immutable rankings for completed calendar weeks, ordered from the most
            recent week to the oldest one. Pagination applies to weeks rather than players.
            """
    )
    @ApiResponse(responseCode = "200", description = "Ranking history returned successfully.")
    @ApiResponse(responseCode = "400", description = "Pagination values are invalid.")
    public PageResponse<RankingHistoryWeekResponse> getRankingHistory(
        @Parameter(description = "Zero-based page index.", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Number of finalized weeks returned per page.", example = "10")
        @RequestParam(defaultValue = "10") int size
    ) {
        return rankingQueryService.findHistory(page, size);
    }
}
