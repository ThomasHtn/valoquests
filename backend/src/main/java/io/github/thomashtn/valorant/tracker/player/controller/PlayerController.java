package io.github.thomashtn.valorant.tracker.player.controller;

import io.github.thomashtn.valorant.tracker.player.dto.PlayerDetailsResponse;
import io.github.thomashtn.valorant.tracker.player.dto.PlayerSummaryResponse;
import io.github.thomashtn.valorant.tracker.player.service.PlayerQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
     * Provider used to access the optional feature service when implemented.
     */
    private final PlayerQueryService service;

    /**
     * @param serviceProvider provider for the future player query implementation
     */
    public PlayerController(PlayerQueryService service) {
        this.service = service;
    }

    /**
     * @return every active or inactive player followed by the application
     */
    @GetMapping
    @Operation(
        summary = "List tracked players",
        description = """
            Returns every player configured in the application with identity, current competitive
            rank, synchronization state and the main statistics used by the player list.
            """
    )
    @ApiResponse(responseCode = "200", description = "Players returned successfully.")
        public List<PlayerSummaryResponse> getPlayers() {
        return service.findAll();
    }

    /**
     * @param playerId internal database identifier of the requested player
     * @param seasonId optional season identifier restricting the statistics
     * @param gameMode optional game mode restricting the statistics
     * @return complete player profile and aggregated statistics
     */
    @GetMapping("/{playerId}")
    @Operation(
        summary = "Get a player profile",
        description = """
            Returns a complete player profile containing Riot identity, current competitive rank,
            global performance indicators and aggregated statistics by agent and map, optionally
            scoped to one season and/or one game mode.
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
        @RequestParam(required = false) String gameMode
    ) {
        return service.findById(playerId, seasonId, gameMode);
    }
}
