package io.github.thomashtn.valoquests.match.controller;

import io.github.thomashtn.valoquests.match.dto.MatchDetailResponse;
import io.github.thomashtn.valoquests.match.dto.MatchResponse;
import io.github.thomashtn.valoquests.match.model.MatchHistoryFilter;
import io.github.thomashtn.valoquests.match.service.MatchQueryService;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
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
     * Application service resolving the filtered match history.
     */
    private final MatchQueryService service;

    /**
     * Creates the match history controller.
     *
     * @param service match query service
     */
    public MatchController(MatchQueryService service) {
        this.service = service;
    }

    /**
     * Returns player matches from newest to oldest.
     *
     * @param playerId internal player identifier
     * @param page     zero-based page index
     * @param size     maximum number of matches returned in one page
     * @param filter   optional season, map, agent, result and game mode filters, bound from the
     *     query string
     * @return one page of matching player-match records
     */
    @GetMapping
    @Operation(
        summary = "Get a player's match history",
        description = """
            Returns the tracked player's matches from newest to oldest. Pagination and optional
            season, map, agent, result and game mode filters can be combined in the same request.
            """
    )
    @ApiResponse(responseCode = "200", description = "Match page returned successfully.")
    @ApiResponse(responseCode = "400", description = "A pagination value or filter is invalid.")
    @ApiResponse(responseCode = "404", description = "The requested player does not exist.")
    public PageResponse<MatchResponse> getPlayerMatches(
        @Parameter(description = "Internal player identifier.", example = "3", required = true)
        @PathVariable long playerId,
        @Parameter(description = "Zero-based page index.", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Maximum number of matches returned in one page.", example = "10")
        @RequestParam(defaultValue = "10") int size,
        @ParameterObject MatchHistoryFilter filter
    ) {
        return service.findByPlayer(playerId, page, size, filter);
    }

    /**
     * Returns full detail for one of the player's matches.
     *
     * @param playerId internal player identifier
     * @param id       internal player-match identifier, matching {@link MatchResponse#id}
     * @return the requested match's full detail
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get full detail for one of a player's matches",
        description = """
            Returns everything stored about one match: the same figures as the history page, plus
            the shot-type breakdown, raw damage, round count and every other tracked player found in
            the same match.
            """
    )
    @ApiResponse(responseCode = "200", description = "Match detail returned successfully.")
    @ApiResponse(
        responseCode = "404",
        description = "The requested player, or the requested match for that player, does not exist."
    )
    public MatchDetailResponse getPlayerMatchDetail(
        @Parameter(description = "Internal player identifier.", example = "3", required = true)
        @PathVariable long playerId,
        @Parameter(description = "Internal player-match identifier.", example = "42", required = true)
        @PathVariable long id
    ) {
        return service.findDetail(playerId, id);
    }
}
