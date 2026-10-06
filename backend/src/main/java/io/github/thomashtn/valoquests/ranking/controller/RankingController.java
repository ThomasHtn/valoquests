package io.github.thomashtn.valoquests.ranking.controller;

import io.github.thomashtn.valoquests.ranking.dto.CurrentRankingResponse;
import io.github.thomashtn.valoquests.ranking.dto.DailyRankingResponse;
import io.github.thomashtn.valoquests.ranking.dto.RankingHistoryWeekResponse;
import io.github.thomashtn.valoquests.ranking.service.RankingQueryService;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
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
     * Creates the public ranking controller.
     *
     * @param rankingQueryService ranking query service
     */
    public RankingController(RankingQueryService rankingQueryService) {
        this.rankingQueryService = rankingQueryService;
    }

    /**
     * Returns the ranking of the week in progress.
     *
     * @return active-week ranking with every player's score and validated challenges
     */
    @GetMapping("/current")
    @Operation(
        summary = "Get the current weekly ranking",
        description = """
            Returns each player's current position, score and validated-challenge counts for the
            active calendar week.
            """
    )
    @ApiResponse(responseCode = "200", description = "Current ranking returned successfully.")
    public CurrentRankingResponse getCurrentRanking() {
        return rankingQueryService.findCurrent();
    }

    /**
     * Returns one day's ranking.
     *
     * @param day day to rank, or absent for today
     * @return one day's ranking
     */
    @GetMapping("/daily")
    @Operation(
        summary = "Get one day's ranking",
        description = """
            Returns every tracked player's match damage for one day, archived players aside. Only
            match damage exists at this scale: challenge points are settled on the week, not on the
            day.
            """
    )
    @ApiResponse(responseCode = "200", description = "Daily ranking returned successfully.")
    public DailyRankingResponse getDailyRanking(
        @Parameter(description = "Day to rank, as YYYY-MM-DD. Defaults to today.", example = "2026-09-01")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day
    ) {
        return rankingQueryService.findDaily(day);
    }

    /**
     * Returns finalized weekly rankings, newest first.
     *
     * @param page zero-based page index
     * @param size number of finalized weeks returned per page
     * @return one page of finalized weekly rankings
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
