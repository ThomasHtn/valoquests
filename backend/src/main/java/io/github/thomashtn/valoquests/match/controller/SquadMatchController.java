package io.github.thomashtn.valoquests.match.controller;

import io.github.thomashtn.valoquests.match.dto.SquadMatchResponse;
import io.github.thomashtn.valoquests.match.service.MatchQueryService;
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
 * Exposes the squad's matches of the day, every roster player at once.
 */
@RestController
@RequestMapping(value = "/api/matches", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Squad matches", description = "Paginated matches of the day for the whole squad.")
public class SquadMatchController {

    /**
     * Application service resolving the squad's match history.
     */
    private final MatchQueryService service;

    /**
     * Creates the squad match history controller.
     *
     * @param service match query service
     */
    public SquadMatchController(MatchQueryService service) {
        this.service = service;
    }

    /**
     * Returns the squad's matches of the day from newest to oldest.
     *
     * @param page zero-based page index
     * @param size maximum number of matches returned in one page
     * @return one page of the squad's player-match records
     */
    @GetMapping
    @Operation(
        summary = "Get the squad's matches of the day",
        description = """
            Returns today's matches of the shown campaign's roster from newest to oldest, each named
            after the player who played it. A match shared by several tracked players appears once
            per player. Empty when no campaign exists.
            """
    )
    @ApiResponse(responseCode = "200", description = "Match page returned successfully.")
    @ApiResponse(responseCode = "400", description = "A pagination value is invalid.")
    public PageResponse<SquadMatchResponse> getSquadMatches(
        @Parameter(description = "Zero-based page index.", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Maximum number of matches returned in one page.", example = "10")
        @RequestParam(defaultValue = "10") int size
    ) {
        return service.findSquad(page, size);
    }
}
