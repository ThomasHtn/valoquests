package io.github.thomashtn.valorant.tracker.synchronization.controller;

import io.github.thomashtn.valorant.tracker.shared.dto.PageResponse;
import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationDetailsResponse;
import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationResponse;
import io.github.thomashtn.valorant.tracker.synchronization.service.SynchronizationCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static io.github.thomashtn.valorant.tracker.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;
import static io.github.thomashtn.valorant.tracker.shared.web.RequiredService.get;

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

    private final ObjectProvider<SynchronizationCommandService> serviceProvider;

    /**
     * Creates the administrative synchronization controller.
     *
     * @param serviceProvider synchronization command-service provider
     */
    public SynchronizationAdminController(
        ObjectProvider<SynchronizationCommandService> serviceProvider
    ) {
        this.serviceProvider = serviceProvider;
    }

    /**
     * Synchronizes all active players.
     *
     * <p>This operation is intentionally unavailable until the
     * single-player workflow has been validated.</p>
     *
     * @return synchronization summary
     */
    @PostMapping("/synchronizations")
    @Operation(
        summary = "Synchronize all active players",
        description = """
            Imports recent data for all active players and then recalculates
            challenge progress and ranking.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Synchronization completed."
        ),
        @ApiResponse(
            responseCode = "401",
            description = "X-Admin-Key header is missing."
        ),
        @ApiResponse(
            responseCode = "403",
            description = "X-Admin-Key value is invalid."
        ),
        @ApiResponse(
            responseCode = "501",
            description = "Global synchronization is not implemented yet."
        )
    })
    public SynchronizationResponse synchronizeAllPlayers() {
        return get(
            serviceProvider,
            "Standard synchronization"
        ).synchronizeAllPlayers();
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
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Player synchronization completed."
        ),
        @ApiResponse(
            responseCode = "401",
            description = "X-Admin-Key header is missing."
        ),
        @ApiResponse(
            responseCode = "403",
            description = "X-Admin-Key value is invalid."
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Tracked player not found."
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Henrik rate limit reached."
        ),
        @ApiResponse(
            responseCode = "502",
            description = "Henrik API request failed."
        )
    })
    public SynchronizationResponse synchronizePlayer(
        @Parameter(
            description = "Internal player identifier.",
            example = "3",
            required = true
        )
        @PathVariable long playerId
    ) {
        return get(
            serviceProvider,
            "Single-player synchronization"
        ).synchronizePlayer(playerId);
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
        return get(
            serviceProvider,
            "Deep synchronization"
        ).requestDeepSynchronizationForAllPlayers();
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
        return get(
            serviceProvider,
            "Single-player deep synchronization"
        ).requestDeepSynchronizationForPlayer(playerId);
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
        return get(
            serviceProvider,
            "Latest synchronization consultation"
        ).findLatest();
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
        return get(
            serviceProvider,
            "Synchronization history consultation"
        ).findHistory(page, size);
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
        return get(
            serviceProvider,
            "Synchronization detail consultation"
        ).findById(synchronizationId);
    }
}
