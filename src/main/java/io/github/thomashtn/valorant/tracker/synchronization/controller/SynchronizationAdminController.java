package io.github.thomashtn.valorant.tracker.synchronization.controller;

import static io.github.thomashtn.valorant.tracker.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;
import static io.github.thomashtn.valorant.tracker.shared.web.RequiredService.get;

import io.github.thomashtn.valorant.tracker.shared.dto.PageResponse;
import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationDetailsResponse;
import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationResponse;
import io.github.thomashtn.valorant.tracker.synchronization.service.SynchronizationCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Exposes protected synchronization commands and monitoring endpoints. */
@RestController
@RequestMapping(value = "/api/admin", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Administration - Synchronizations", description = "Manual synchronization commands and execution monitoring.")
@SecurityRequirement(name = ADMIN_KEY_SECURITY_SCHEME)
public class SynchronizationAdminController {

    private final ObjectProvider<SynchronizationCommandService> serviceProvider;

    /** @param serviceProvider provider for the future synchronization implementation */
    public SynchronizationAdminController(ObjectProvider<SynchronizationCommandService> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    /** @return standard synchronization result for all active players */
    @PostMapping("/synchronizations")
    @Operation(summary = "Synchronize all active players", description = "Imports recent data and then recalculates challenge progress and ranking.")
    @ApiResponse(responseCode = "200", description = "Synchronization completed, possibly with partial player failures.")
    @ApiResponse(responseCode = "401", description = "X-Admin-Key header is missing.")
    @ApiResponse(responseCode = "403", description = "X-Admin-Key value is invalid.")
    public SynchronizationResponse synchronizeAllPlayers() {
        return get(serviceProvider, "Standard synchronization").synchronizeAllPlayers();
    }

    /** @return synchronization result for one player */
    @PostMapping("/players/{playerId}/synchronizations")
    @Operation(summary = "Synchronize one player", description = "Imports recent data for one player and recalculates the global ranking.")
    public SynchronizationResponse synchronizePlayer(
        @Parameter(description = "Internal player identifier.", example = "3", required = true)
        @PathVariable long playerId
    ) {
        return get(serviceProvider, "Single-player synchronization").synchronizePlayer(playerId);
    }

    /** @return accepted asynchronous deep-synchronization request */
    @PostMapping("/synchronizations/deep")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Request deep synchronization for all players", description = "Creates resumable background tasks that progressively import the active season.")
    public SynchronizationResponse requestDeepSynchronizationForAllPlayers() {
        return get(serviceProvider, "Deep synchronization").requestDeepSynchronizationForAllPlayers();
    }

    /** @return accepted asynchronous deep-synchronization request for one player */
    @PostMapping("/players/{playerId}/synchronizations/deep")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Request deep synchronization for one player", description = "Creates or resumes the active-season deep import task for one player.")
    public SynchronizationResponse requestDeepSynchronizationForPlayer(
        @Parameter(description = "Internal player identifier.", example = "3", required = true)
        @PathVariable long playerId
    ) {
        return get(serviceProvider, "Single-player deep synchronization")
            .requestDeepSynchronizationForPlayer(playerId);
    }

    /** @return latest synchronization summary */
    @GetMapping("/synchronizations/latest")
    @Operation(summary = "Get latest synchronization", description = "Returns the last attempt and the timestamp of the last successful synchronization.")
    public SynchronizationResponse getLatestSynchronization() {
        return get(serviceProvider, "Latest synchronization consultation").findLatest();
    }

    /** @return paginated synchronization history */
    @GetMapping("/synchronizations")
    @Operation(summary = "Get synchronization history", description = "Returns automatic and manual synchronization executions.")
    public PageResponse<SynchronizationResponse> getSynchronizationHistory(
        @Parameter(description = "Zero-based page index.", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Number of executions returned per page.", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        return get(serviceProvider, "Synchronization history consultation").findHistory(page, size);
    }

    /** @return detailed synchronization execution */
    @GetMapping("/synchronizations/{synchronizationId}")
    @Operation(summary = "Get synchronization details", description = "Returns global data and one result entry for every processed player.")
    public SynchronizationDetailsResponse getSynchronizationDetails(
        @Parameter(description = "Internal synchronization identifier.", example = "25", required = true)
        @PathVariable long synchronizationId
    ) {
        return get(serviceProvider, "Synchronization detail consultation").findById(synchronizationId);
    }
}
