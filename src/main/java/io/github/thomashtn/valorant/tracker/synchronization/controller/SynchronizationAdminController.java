package io.github.thomashtn.valorant.tracker.synchronization.controller;

import io.github.thomashtn.valorant.tracker.shared.dto.PageResponse;
import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationDetailsResponse;
import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationResponse;
import io.github.thomashtn.valorant.tracker.synchronization.service.SynchronizationCommandService;
import io.github.thomashtn.valorant.tracker.synchronization.service.SynchronizationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static io.github.thomashtn.valorant.tracker.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

/**
 * Exposes protected synchronization commands and monitoring endpoints.
 */
@RestController
@RequestMapping(
    value = "/api/admin",
    produces = MediaType.APPLICATION_JSON_VALUE
)
@Tag(
    name = "Administration - Synchronizations",
    description = "Manual synchronization commands and execution monitoring."
)
@SecurityRequirement(name = ADMIN_KEY_SECURITY_SCHEME)
public class SynchronizationAdminController {

    /**
     * Application service used to execute synchronization commands.
     */
    private final SynchronizationCommandService synchronizationService;
    /**
     * Application service used to query synchronization history.
     */
    private final SynchronizationQueryService synchronizationQueryService;

    /**
     * Creates the administrative synchronization controller.
     *
     * @param synchronizationService synchronization command service
     * @param synchronizationQueryService synchronization query service
     */
    public SynchronizationAdminController(
        SynchronizationCommandService synchronizationService,
        SynchronizationQueryService synchronizationQueryService
    ) {
        this.synchronizationService = synchronizationService;
        this.synchronizationQueryService = synchronizationQueryService;
    }

    /**
     * Synchronizes all active players.
     *
     * @return synchronization summary
     */
    @PostMapping("/synchronizations")
    @Operation(
        summary = "Synchronize all active players",
        description = """
            Resolves account data, updates current ranks and imports new recent
            matches for every active tracked player.
            """
    )
    @ApiResponse(
            responseCode = "200",
        description = "Synchronization completed."
    )
    @ApiResponse(
            responseCode = "401",
        description = "X-Admin-Key header is missing."
    )
    @ApiResponse(
            responseCode = "403",
        description = "X-Admin-Key value is invalid."
    )
    public SynchronizationResponse synchronizeAllPlayers() {
        return synchronizationService.synchronizeAllPlayers();
    }

    /**
     * Synchronizes one tracked player.
     *
     * @param playerId internal player identifier
     * @return completed synchronization summary
     */
    @PostMapping("/players/{playerId}/synchronizations")
    @Operation(
        summary = "Synchronize one player",
        description = """
            Resolves the Riot account, updates the current competitive rank,
            imports recent completed matches and records the synchronization.
            """
    )
    @ApiResponse(
            responseCode = "200",
        description = "Player synchronization completed."
    )
    @ApiResponse(
            responseCode = "401",
        description = "X-Admin-Key header is missing."
    )
    @ApiResponse(
            responseCode = "403",
        description = "X-Admin-Key value is invalid."
    )
    @ApiResponse(
            responseCode = "404",
        description = "Tracked player not found."
    )
    @ApiResponse(
            responseCode = "429",
        description = "Henrik rate limit reached."
    )
    @ApiResponse(
            responseCode = "502",
        description = "Henrik API request failed."
    )
    public SynchronizationResponse synchronizePlayer(
        @Parameter(
            description = "Internal player identifier.",
            example = "3",
            required = true
    )
        @PathVariable long playerId
    ) {
        return synchronizationService.synchronizePlayer(playerId);
    }

    /**
     * Requests a deep synchronization for every active player.
     *
     * @return accepted synchronization request
     */
    @PostMapping("/synchronizations/deep")
    @Operation(
        summary = "Deep synchronize all players",
        description = """
            Retrieves every match-history page currently available through Henrik
            for all tracked players. One player failure does not stop the others.
            """
    )
    public SynchronizationResponse requestDeepSynchronizationForAllPlayers() {
        return synchronizationService.requestDeepSynchronizationForAllPlayers();
    }

    /**
     * Requests a deep synchronization for one player.
     *
     * @param playerId internal player identifier
     * @return accepted synchronization request
     */
    @PostMapping("/players/{playerId}/synchronizations/deep")
    @Operation(
        summary = "Deep synchronize one player",
        description = """
            Retrieves every match-history page currently available through Henrik
            for one tracked player.
            """
    )
    public SynchronizationResponse requestDeepSynchronizationForPlayer(
        @Parameter(
            description = "Internal player identifier.",
            example = "3",
            required = true
    )
        @PathVariable long playerId
    ) {
        return synchronizationService.requestDeepSynchronizationForPlayer(playerId);
    }

    /**
     * Returns the latest synchronization attempt.
     *
     * @return latest synchronization summary
     */
    @GetMapping("/synchronizations/latest")
    @Operation(
        summary = "Get latest synchronization",
        description = """
            Returns the last attempt and the timestamp of the last successful
            synchronization.
            """
    )
    public SynchronizationResponse getLatestSynchronization() {
        return synchronizationQueryService.findLatest();
    }

    /**
     * Returns the synchronization history.
     *
     * @param page zero-based page index
     * @param size requested page size
     * @return paginated synchronization history
     */
    @GetMapping("/synchronizations")
    @Operation(
        summary = "Get synchronization history",
        description = "Returns automatic and manual synchronization executions."
    )
    public PageResponse<SynchronizationResponse> getSynchronizationHistory(
        @Parameter(
            description = "Zero-based page index.",
            example = "0"
    )
        @RequestParam(defaultValue = "0") int page,

        @Parameter(
            description = "Number of executions returned per page.",
            example = "20"
    )
        @RequestParam(defaultValue = "20") int size
    ) {
        return synchronizationQueryService.findHistory(page, size);
    }

    /**
     * Returns one synchronization execution with its player results.
     *
     * @param synchronizationId synchronization identifier
     * @return detailed synchronization execution
     */
    @GetMapping("/synchronizations/{synchronizationId}")
    @Operation(
        summary = "Get synchronization details",
        description = """
            Returns global synchronization data and one result entry for every
            processed player.
            """
    )
    public SynchronizationDetailsResponse getSynchronizationDetails(
        @Parameter(
            description = "Internal synchronization identifier.",
            example = "25",
            required = true
    )
        @PathVariable long synchronizationId
    ) {
        return synchronizationQueryService.findById(synchronizationId);
    }
}
