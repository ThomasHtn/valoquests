package io.github.thomashtn.valorant.tracker.synchronization.controller;

import static io.github.thomashtn.valorant.tracker.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

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
            Imports every match of the current Valorant season for each active tracked player. The
            operation resolves missing Riot account identifiers, refreshes competitive ranks and walks
            the Henrik match history backwards until it leaves the current season, importing matches
            idempotently. Modes the tracker does not follow, such as Swiftplay, Escalation, New Map
            and custom games, are skipped; a queue this application cannot classify is imported so it
            is never lost. Once a season has been walked in full, later runs stop at the first
            already-stored match. A season left unfinished by an interruption, or a season the player
            was still catching up when Riot rolled the act over, is walked again in full rather than
            stopped early, so the history can never keep a hole. A failure for one player does not
            prevent the remaining players from being processed.

            Matches played in an act older than the current one are never imported, and no command
            backfills them: a player's stored history therefore starts at the current act, and its
            match counts are expected to be lower than the lifetime totals shown by external trackers.
            Every player result reports the condition that ended its walk, which tells a run that
            exhausted the current act apart from one truncated by the safety page limit.

            When the run imported at least one match, the current week's challenge progress and the
            weekly ranking are rebuilt from the stored matches before the response is returned. A
            failure of that step is logged without failing the synchronization, since the matches are
            already stored and the next run recalculates from scratch.
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
            Imports every match of the current Valorant season for one tracked player, applying the
            same season scope, mode filter and early-stop rules as the batch operation. Useful to
            catch up a single player that failed during a scheduled run, without replaying the others.
            Older acts are out of scope here too, so this command cannot deepen an existing history.
            Challenge progress and the weekly ranking are rebuilt for every player when the run
            imported at least one match.
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
     * Returns the latest synchronization attempt.
     *
     * @return latest synchronization summary
     */
    @GetMapping("/synchronizations/latest")
    @Operation(
        summary = "Get latest synchronization",
        description = """
            Returns the most recently created synchronization execution, including its type, trigger,
            status, counters, error message and the latest successful synchronization timestamp.
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
        description = """
            Returns a page of manual and scheduled synchronization executions ordered from newest to
            oldest. Each item contains execution status, processed-player counters and imported matches.
            """
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
            Returns one synchronization execution with its global counters and one detailed result for
            every processed player, including imported matches and any failure message.
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
