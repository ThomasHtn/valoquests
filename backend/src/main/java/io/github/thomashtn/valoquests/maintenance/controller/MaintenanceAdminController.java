package io.github.thomashtn.valoquests.maintenance.controller;

import static io.github.thomashtn.valoquests.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import io.github.thomashtn.valoquests.maintenance.service.CampaignResetService;
import io.github.thomashtn.valoquests.shared.concurrency.MatchHistoryLock;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the protected destructive maintenance operations.
 */
@RestController
@RequestMapping("/api/admin/maintenance")
@Tag(name = "Administration - Maintenance", description = "Destructive maintenance operations.")
@SecurityRequirement(name = ADMIN_KEY_SECURITY_SCHEME)
public class MaintenanceAdminController {

    /**
     * Service wiping the data derived from match history.
     */
    private final CampaignResetService campaignResetService;

    /**
     * Lock keeping the reset from emptying tables a synchronization or a rollover is writing.
     */
    private final MatchHistoryLock matchHistoryLock;

    /**
     * Creates the administrative maintenance controller.
     *
     * @param campaignResetService campaign reset service
     * @param matchHistoryLock     lock shared by every job writing the match history
     */
    public MaintenanceAdminController(
        CampaignResetService campaignResetService,
        MatchHistoryLock matchHistoryLock
    ) {
        this.campaignResetService = campaignResetService;
        this.matchHistoryLock = matchHistoryLock;
    }

    /**
     * Clears every record derived from match history.
     */
    @PostMapping("/campaign-reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Reset every campaign to an empty match history",
        description = """
            Irreversibly deletes every match, every challenge selection and its progress, every
            weekly ranking, every campaign with its weeks, roster and snapshots, and the whole
            synchronization history, then rewinds each player's synchronization watermark so the
            next run re-imports from scratch.

            The player roster, the challenge catalogue and the guardian catalogue are kept: none of
            them is derived from match history.

            Refused with a 409 while a synchronization or a weekly rollover is running, which would
            otherwise be writing into the tables being emptied.
            """
    )
    @ApiResponse(responseCode = "204", description = "Campaign data cleared.")
    @ApiResponse(responseCode = "401", description = "X-Admin-Key header is missing.")
    @ApiResponse(responseCode = "403", description = "X-Admin-Key value is invalid.")
    @ApiResponse(
        responseCode = "409",
        description = "Another job writing the match history is already running."
    )
    public void resetCampaign() {
        // Taken outside the reset transaction so it is only released once the reset committed.
        matchHistoryLock.runOrReject(campaignResetService::resetCampaign);
    }
}
