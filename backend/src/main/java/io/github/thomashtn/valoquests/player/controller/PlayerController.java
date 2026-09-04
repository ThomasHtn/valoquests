package io.github.thomashtn.valoquests.player.controller;

import io.github.thomashtn.valoquests.player.dto.PlayerContributionResponse;
import io.github.thomashtn.valoquests.player.dto.PlayerDetailsResponse;
import io.github.thomashtn.valoquests.player.dto.PlayerProgressionResponse;
import io.github.thomashtn.valoquests.player.dto.PlayerSummaryResponse;
import io.github.thomashtn.valoquests.player.service.PlayerContributionQueryService;
import io.github.thomashtn.valoquests.player.service.PlayerProgressionQueryService;
import io.github.thomashtn.valoquests.player.service.PlayerQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes player list and player-profile consultation endpoints.
 */
@RestController
@RequestMapping(value = "/api/players", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Players", description = "Tracked Valorant player identities and aggregated statistics.")
public class PlayerController {

    /**
     * Application service resolving tracked players and their statistics.
     */
    private final PlayerQueryService service;

    /**
     * Application service resolving the analytics behind the progression view.
     */
    private final PlayerProgressionQueryService progressionService;

    /**
     * Application service resolving what a player brings to the week and the campaign.
     */
    private final PlayerContributionQueryService contributionService;

    /**
     * Creates the player controller.
     *
     * @param service             player query service
     * @param progressionService  progression analytics query service
     * @param contributionService contribution query service
     */
    public PlayerController(
        PlayerQueryService service,
        PlayerProgressionQueryService progressionService,
        PlayerContributionQueryService contributionService
    ) {
        this.service = service;
        this.progressionService = progressionService;
        this.contributionService = contributionService;
    }

    /**
     * Lists every tracked player.
     *
     * @return every active or inactive player followed by the application
     */
    @GetMapping
    @Operation(
        summary = "List tracked players",
        description = """
            Returns every player configured in the application with identity, current competitive
            rank, synchronization state and the main statistics used by the player list, scoped to
            the season currently in progress and to competitive matches.
            """
    )
    @ApiResponse(responseCode = "200", description = "Players returned successfully.")
        public List<PlayerSummaryResponse> getPlayers() {
        return service.findAll();
    }

    /**
     * @param playerId  internal database identifier of the requested player
     * @param seasonId  optional season identifier restricting the statistics
     * @param gameMode  optional game mode restricting the statistics
     * @param weekStart optional Monday restricting the statistics to that calendar week
     * @return complete player profile and aggregated statistics
     */
    @GetMapping("/{playerId}")
    @Operation(
        summary = "Get a player profile",
        description = """
            Returns a complete player profile containing Riot identity, current competitive rank,
            global performance indicators and aggregated statistics by agent and map, optionally
            scoped to one season, one game mode and/or one calendar week.
            """
    )
    @ApiResponse(responseCode = "200", description = "Player profile returned successfully.")
    @ApiResponse(responseCode = "404", description = "No tracked player exists for the supplied identifier.")
        public PlayerDetailsResponse getPlayer(
        @Parameter(description = "Internal player identifier.", example = "3", required = true)
        @PathVariable long playerId,
        @Parameter(description = "Restricts statistics to one season. Omit for every season.")
        @RequestParam(required = false) Long seasonId,
        @Parameter(description = "Restricts statistics to one game mode. Omit for every mode.")
        @RequestParam(required = false) String gameMode,
        @Parameter(description = "Restricts statistics to the calendar week starting on this Monday. "
            + "Omit for every week.")
        @RequestParam(required = false) LocalDate weekStart
    ) {
        return service.findById(playerId, seasonId, gameMode, weekStart);
    }

    /**
     * @param playerId  internal database identifier of the requested player
     * @param seasonIds optional seasons restricting the analytics
     * @return every analytic the progression view renders
     */
    @GetMapping("/{playerId}/progression")
    @Operation(
        summary = "Get a player's progression analytics",
        description = """
            Returns the analytics behind the player profile's progression view: match-by-match
            evolution per season, where the player's hits land, performance per weekday and time
            slot, personal records, and aggregated statistics by map and agent. Every figure is
            scoped to competitive matches, except the longest run of consecutive active days,
            which counts any game mode.
            """
    )
    @ApiResponse(responseCode = "200", description = "Progression analytics returned successfully.")
    @ApiResponse(responseCode = "404", description = "No tracked player exists for the supplied identifier.")
        public PlayerProgressionResponse getPlayerProgression(
        @Parameter(description = "Internal player identifier.", example = "3", required = true)
        @PathVariable long playerId,
        @Parameter(description = "Restricts the analytics to these seasons. Omit for every season.")
        @RequestParam(required = false) List<Long> seasonIds
    ) {
        return progressionService.findByPlayerId(playerId, seasonIds);
    }

    /**
     * @param playerId internal database identifier of the requested player
     * @return the player's contribution to the current week and to the campaign in progress
     */
    @GetMapping("/{playerId}/contribution")
    @Operation(
        summary = "Get a player's contribution",
        description = """
            Returns what the player brings to the squad: their current week as the ranking holds it
            (guardian damage, resources, streak, validated challenges, honours) and, when a campaign
            is live and they are on its roster, their whole campaign so far.
            """
    )
    @ApiResponse(responseCode = "200", description = "Player contribution returned successfully.")
    @ApiResponse(responseCode = "404", description = "No tracked player exists for the supplied identifier.")
    public PlayerContributionResponse getPlayerContribution(
        @Parameter(description = "Internal player identifier.", example = "3", required = true)
        @PathVariable long playerId
    ) {
        return contributionService.findByPlayerId(playerId);
    }
}
