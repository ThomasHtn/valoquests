package io.github.thomashtn.valorant.tracker.player.controller;

import static io.github.thomashtn.valorant.tracker.shared.web.RequiredService.get;

import io.github.thomashtn.valorant.tracker.player.dto.PlayerDetailsResponse;
import io.github.thomashtn.valorant.tracker.player.dto.PlayerSummaryResponse;
import io.github.thomashtn.valorant.tracker.player.service.PlayerQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes player list and player-profile consultation endpoints. */
@RestController
@RequestMapping(value = "/api/players", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Players", description = "Tracked Valorant player identities and aggregated statistics.")
public class PlayerController {

    private final ObjectProvider<PlayerQueryService> serviceProvider;

    /** @param serviceProvider provider for the future player query implementation */
    public PlayerController(ObjectProvider<PlayerQueryService> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    /** @return every active or inactive player followed by the application */
    @GetMapping
    @Operation(summary = "List tracked players", description = "Returns compact player cards and their main aggregated statistics.")
    @ApiResponse(responseCode = "200", description = "Players returned successfully.")
    @ApiResponse(responseCode = "501", description = "Player query service has not been implemented yet.")
    public List<PlayerSummaryResponse> getPlayers() {
        return get(serviceProvider, "Player list consultation").findAll();
    }

    /**
     * @param playerId internal database identifier of the requested player
     * @return complete player profile and aggregated statistics
     */
    @GetMapping("/{playerId}")
    @Operation(summary = "Get a player profile", description = "Returns identity, rank, global statistics, agent statistics and map statistics.")
    @ApiResponse(responseCode = "200", description = "Player profile returned successfully.")
    @ApiResponse(responseCode = "404", description = "No tracked player exists for the supplied identifier.")
    @ApiResponse(responseCode = "501", description = "Player query service has not been implemented yet.")
    public PlayerDetailsResponse getPlayer(
        @Parameter(description = "Internal player identifier.", example = "3", required = true)
        @PathVariable long playerId
    ) {
        return get(serviceProvider, "Player profile consultation").findById(playerId);
    }
}
