package io.github.thomashtn.valoquests.maintenance.controller;

import static io.github.thomashtn.valoquests.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import io.github.thomashtn.valoquests.maintenance.service.CampaignResetService;
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
     * Creates the administrative maintenance controller.
     *
     * @param campaignResetService campaign reset service
     */
    public MaintenanceAdminController(CampaignResetService campaignResetService) {
        this.campaignResetService = campaignResetService;
    }

    /**
     * Clears every record derived from match history.
     */
    @PostMapping("/campaign-reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Start a new campaign from an empty base",
        description = """
            Irreversibly deletes every match, every weekly challenge selection and its progress,
            every weekly ranking, every boss encounter and the whole synchronization history, then
            rewinds each player's synchronization watermark so the next run re-imports from scratch.

            The player roster, the challenge catalogue and the boss catalogue are kept: none of them
            is derived from match history.

            Refused with a 409 while a synchronization is running, which would otherwise be writing
            matches into the base being emptied.
            """
    )
    @ApiResponse(responseCode = "204", description = "Campaign data cleared.")
    @ApiResponse(responseCode = "401", description = "X-Admin-Key header is missing.")
    @ApiResponse(responseCode = "403", description = "X-Admin-Key value is invalid.")
    @ApiResponse(responseCode = "409", description = "A synchronization is in progress.")
    public void resetCampaign() {
        campaignResetService.resetCampaign();
    }
}
