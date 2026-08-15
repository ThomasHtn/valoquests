package io.github.thomashtn.valorant.tracker.synchronization.controller;

import static io.github.thomashtn.valorant.tracker.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import io.github.thomashtn.valorant.tracker.shared.dto.PageResponse;
import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationDetailsResponse;
import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationResponse;
import io.github.thomashtn.valorant.tracker.synchronization.service.SynchronizationLaunchService;
import io.github.thomashtn.valorant.tracker.synchronization.service.SynchronizationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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
     * Application service used to accept and dispatch synchronization commands.
     */
    private final SynchronizationLaunchService synchronizationLaunchService;

    /**
     * Application service used to query synchronization history.
     */
    private final SynchronizationQueryService synchronizationQueryService;

    /**
     * Creates the administrative synchronization controller.
     *
     * @param synchronizationLaunchService synchronization launch service
     * @param synchronizationQueryService synchronization query service
     */
    public SynchronizationAdminController(
        SynchronizationLaunchService synchronizationLaunchService,
        SynchronizationQueryService synchronizationQueryService
    ) {
        this.synchronizationLaunchService = synchronizationLaunchService;
        this.synchronizationQueryService = synchronizationQueryService;
    }

    /**
     * Accepts a synchronization of all tracked players.
     */
    @PostMapping("/synchronizations")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
        summary = "Start a synchronization of every tracked player",
        description = """
            Accepts the request and runs the synchronization in the background, then answers
            immediately. A full run walks the Henrik match history for the whole squad under a rate
            limit of a few dozen requests per minute and routinely takes minutes, which no HTTP
            client should be asked to wait through. Poll
            `GET /api/admin/synchronizations/latest` to follow the run: it reports PENDING or
            RUNNING while it is in flight, then its final status and counters.

            Only one execution may be in flight at a time; a second request is refused with a 409
            rather than queued, so two walks never spend the same rate-limit budget.

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
            weekly ranking are rebuilt from the stored matches once the walk completes. A failure of
            that step is logged without failing the synchronization, since the matches are already
            stored and the next run recalculates from scratch.
            """
    )
    @ApiResponse(
            responseCode = "202",
        description = "Synchronization accepted and started in the background."
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
            responseCode = "409",
        description = "A synchronization is already in progress."
    )
    public void synchronizeAllPlayers() {
        synchronizationLaunchService.launchAllPlayers();
    }

    /**
     * Accepts a synchronization of one tracked player.
     *
     * @param playerId internal player identifier
     */
    @PostMapping("/players/{playerId}/synchronizations")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
        summary = "Start a synchronization of one player",
        description = """
            Imports every match of the current Valorant season for one tracked player, applying the
            same season scope, mode filter and early-stop rules as the batch operation. Useful to
            catch up a single player that failed during a scheduled run, without replaying the others.
            Older acts are out of scope here too, so this command cannot deepen an existing history.
            Challenge progress and the weekly ranking are rebuilt for every player when the run
            imported at least one match.

            Runs in the background like the batch operation, and is followed the same way through
            `GET /api/admin/synchronizations/latest`. The player is resolved before the request is
            accepted, so an unknown identifier is reported right away instead of surfacing minutes
            later as a failed execution. Henrik failures, in contrast, can only be observed on that
            execution: they happen long after this route has answered.
            """
    )
    @ApiResponse(
            responseCode = "202",
        description = "Player synchronization accepted and started in the background."
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
            responseCode = "409",
        description = "A synchronization is already in progress."
    )
    public void synchronizePlayer(
        @Parameter(
            description = "Internal player identifier.",
            example = "3",
            required = true
    )
        @PathVariable long playerId
    ) {
        synchronizationLaunchService.launchPlayer(playerId);
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
