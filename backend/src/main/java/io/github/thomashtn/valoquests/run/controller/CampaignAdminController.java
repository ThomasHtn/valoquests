package io.github.thomashtn.valoquests.run.controller;

import static io.github.thomashtn.valoquests.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.run.dto.CampaignAdminResponse;
import io.github.thomashtn.valoquests.run.dto.CampaignAdminResponse.CampaignRunSummary;
import io.github.thomashtn.valoquests.run.dto.CampaignAutoRenewUpdateRequest;
import io.github.thomashtn.valoquests.run.service.CampaignAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the campaign's lifecycle to an operator: every run, current or closed, starting and
 * stopping the current one, and switching automatic renewal.
 */
@RestController
@RequestMapping(value = "/api/admin/campaigns", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Administration - Campaigns", description = "Campaign lifecycle management.")
@SecurityRequirement(name = ADMIN_KEY_SECURITY_SCHEME)
public class CampaignAdminController {

    /**
     * Application service backing the campaign lifecycle.
     */
    private final CampaignAdminService service;

    /**
     * Creates the campaign admin controller.
     *
     * @param service campaign admin service
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public CampaignAdminController(CampaignAdminService service) {
        this.service = service;
    }

    /**
     * Lists every run of the campaign and the automatic-renewal setting.
     *
     * @return the campaign's lifecycle
     */
    @GetMapping
    @Operation(
        summary = "List the campaign's runs",
        description = """
            Returns the run in progress if there is one, every closed run, and whether the weekly
            rollover may open the next run on its own.
            """
    )
    @ApiResponse(responseCode = "200", description = "Campaign lifecycle returned successfully.")
    public CampaignAdminResponse getCampaigns() {
        return service.findCampaigns();
    }

    /**
     * Switches automatic renewal on or off.
     *
     * @param request the setting to apply
     */
    @PatchMapping(value = "/auto-renew", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Switch automatic renewal",
        description = """
            Turned off, a run closing — naturally or stopped early — leaves "no campaign" open until
            an operator starts the next one, instead of the weekly rollover opening it on its own.
            """
    )
    @ApiResponse(responseCode = "200", description = "Setting applied successfully.")
    public void setAutoRenew(@Valid @RequestBody CampaignAutoRenewUpdateRequest request) {
        service.setAutoRenewEnabled(request.enabled());
    }

    /**
     * Starts a new run today.
     *
     * @return the started run
     */
    @PostMapping("/start")
    @Operation(
        summary = "Start a new campaign",
        description = """
            Opens a run on this week's Monday, freezing the active roster's size against it. Only
            for the gap automatic renewal being off deliberately leaves open — a live campaign is
            otherwise opened lazily by the weekly rollover.
            """
    )
    @ApiResponse(responseCode = "200", description = "Campaign started successfully.")
    @ApiResponse(responseCode = "409", description = "A campaign is already running.")
    public CampaignRunSummary startCampaign() {
        return service.startCampaign();
    }

    /**
     * Stops the run in progress today.
     *
     * @return the stopped run
     */
    @PostMapping("/stop")
    @Operation(
        summary = "Stop the current campaign",
        description = """
            Closes the run in progress today, freezing its score at today rather than at its
            settlement day. Irreversible: the weeks it never reached are simply never played.
            """
    )
    @ApiResponse(responseCode = "200", description = "Campaign stopped successfully.")
    @ApiResponse(responseCode = "409", description = "No campaign is currently running.")
    public CampaignRunSummary stopCampaign() {
        return service.stopCampaign();
    }

    /**
     * Deletes one campaign, running or closed.
     *
     * @param id identifier of the run to delete
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Delete a campaign",
        description = """
            Irreversibly deletes one campaign: the run itself, the colony it grew and the boss
            fights it drew. The campaign in progress can be deleted like any other, which leaves no
            campaign running at all until one is started again.

            Matches, weekly challenges and weekly rankings are kept — they belong to the weekly
            ranking, which is read whether a campaign is running or not. Clearing those is what the
            maintenance reset is for.
            """
    )
    @ApiResponse(responseCode = "204", description = "Campaign deleted successfully.")
    @ApiResponse(responseCode = "404", description = "No campaign holds that identifier.")
    public void deleteCampaign(@PathVariable long id) {
        service.deleteCampaign(id);
    }
}
