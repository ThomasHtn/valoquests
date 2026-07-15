package io.github.thomashtn.valorant.tracker.ranking.controller;

import static io.github.thomashtn.valorant.tracker.shared.web.RequiredService.get;

import io.github.thomashtn.valorant.tracker.ranking.dto.CurrentRankingResponse;
import io.github.thomashtn.valorant.tracker.ranking.dto.RankingHistoryWeekResponse;
import io.github.thomashtn.valorant.tracker.ranking.service.RankingQueryService;
import io.github.thomashtn.valorant.tracker.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the active ranking and finalized weekly ranking history. */
@RestController
@RequestMapping(value = "/api/rankings", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Rankings", description = "Current weekly standings and finalized historical standings.")
public class RankingController {

    private final ObjectProvider<RankingQueryService> serviceProvider;

    /** @param serviceProvider provider for the future ranking query implementation */
    public RankingController(ObjectProvider<RankingQueryService> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    /** @return active-week ranking with exact progress for every player and challenge */
    @GetMapping("/current")
    @Operation(summary = "Get current weekly ranking", description = "Returns player positions, points and exact per-challenge progress for the active week.")
    @ApiResponse(responseCode = "200", description = "Current ranking returned successfully.")
    @ApiResponse(responseCode = "501", description = "Ranking query service has not been implemented yet.")
    public CurrentRankingResponse getCurrentRanking() {
        return get(serviceProvider, "Current ranking consultation").findCurrent();
    }

    /** @return one page of finalized weekly rankings */
    @GetMapping("/history")
    @Operation(summary = "Get ranking history", description = "Returns past finalized weeks ordered from the most recent to the oldest.")
    @ApiResponse(responseCode = "200", description = "Ranking history returned successfully.")
    @ApiResponse(responseCode = "400", description = "Pagination values are invalid.")
    @ApiResponse(responseCode = "501", description = "Ranking query service has not been implemented yet.")
    public PageResponse<RankingHistoryWeekResponse> getRankingHistory(
        @Parameter(description = "Zero-based page index.", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Number of finalized weeks returned per page.", example = "10")
        @RequestParam(defaultValue = "10") int size
    ) {
        return get(serviceProvider, "Ranking history consultation").findHistory(page, size);
    }
}
