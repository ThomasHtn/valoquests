package io.github.thomashtn.valorant.tracker.match.controller;

import static io.github.thomashtn.valorant.tracker.shared.web.RequiredService.get;

import io.github.thomashtn.valorant.tracker.match.dto.MatchResponse;
import io.github.thomashtn.valorant.tracker.match.service.MatchQueryService;
import io.github.thomashtn.valorant.tracker.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes paginated match history for tracked players.
 */
@RestController
@RequestMapping(value = "/api/players/{playerId}/matches", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Matches", description = "Paginated and filterable match history for one tracked player.")
public class MatchController {

    /**
     * Provider used to access the optional feature service when implemented.
     */
    private final ObjectProvider<MatchQueryService> serviceProvider;

    /**
     * @param serviceProvider provider for the future match query implementation
     */
    public MatchController(ObjectProvider<MatchQueryService> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    /**
     * Returns player matches from newest to oldest.
     *
     * @return one page of matching player-match records
     */
    @GetMapping
    @Operation(
        summary = "Get a player's match history",
        description = """
            Returns the tracked player's matches from newest to oldest. Pagination and optional
            season, map, agent and result filters can be combined in the same request.
            """
    )
    @ApiResponse(responseCode = "200", description = "Match page returned successfully.")
    @ApiResponse(responseCode = "400", description = "A pagination value or filter is invalid.")
    @ApiResponse(responseCode = "404", description = "The requested player does not exist.")
    @ApiResponse(responseCode = "501", description = "Match query service has not been implemented yet.")
    public PageResponse<MatchResponse> getPlayerMatches(
        @Parameter(description = "Internal player identifier.", example = "3", required = true)
        @PathVariable long playerId,
        @Parameter(description = "Zero-based page index.", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Maximum number of matches returned in one page.", example = "10")
        @RequestParam(defaultValue = "10") int size,
        @Parameter(description = "Optional internal season identifier.", example = "8")
        @RequestParam(required = false) Long seasonId,
        @Parameter(description = "Optional exact map name.", example = "Ascent")
        @RequestParam(required = false) String map,
        @Parameter(description = "Optional exact agent name.", example = "Omen")
        @RequestParam(required = false) String agent,
        @Parameter(description = "Optional result filter: WIN, LOSS or DRAW.", example = "WIN")
        @RequestParam(required = false) String result
    ) {
        return get(serviceProvider, "Player match history consultation")
            .findByPlayer(playerId, page, size, seasonId, map, agent, result);
    }
}
