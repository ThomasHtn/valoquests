package io.github.thomashtn.valoquests.campaign.controller;

import static io.github.thomashtn.valoquests.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.dto.CampaignAdminResponse;
import io.github.thomashtn.valoquests.campaign.dto.SquadCalibrationResponse;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.model.SquadCalibration;
import io.github.thomashtn.valoquests.campaign.service.AsyncHistoryBackfillRunner;
import io.github.thomashtn.valoquests.campaign.service.CampaignLifecycleService;
import io.github.thomashtn.valoquests.campaign.service.CampaignReplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the protected campaign lifecycle: calibrate, backfill, open, stop, replay, delete.
 *
 * <p>Nothing here happens on its own. A campaign is opened by a person who has looked at the
 * calibration first, and the ten weeks that follow are decided in that single moment.
 */
@RestController
@RequestMapping("/api/admin/campaigns")
@Tag(name = "Administration - Campaigns", description = "Campaign lifecycle and history backfill.")
@SecurityRequirement(name = ADMIN_KEY_SECURITY_SCHEME)
public class CampaignAdminController {

    /**
     * Service opening, starting and stopping campaigns.
     */
    private final CampaignLifecycleService lifecycleService;

    /**
     * Service replaying the campaign in progress.
     */
    private final CampaignReplayService replayService;

    /**
     * Runner walking the calibration window in the background.
     */
    private final AsyncHistoryBackfillRunner backfillRunner;

    /**
     * Clock stamping the closing instant.
     */
    private final Clock clock;

    /**
     * Creates the campaign administration controller.
     *
     * @param lifecycleService campaign lifecycle service
     * @param replayService    campaign replay service
     * @param backfillRunner   asynchronous history backfill runner
     * @param clock            clock
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public CampaignAdminController(
        CampaignLifecycleService lifecycleService,
        CampaignReplayService replayService,
        AsyncHistoryBackfillRunner backfillRunner,
        Clock clock
    ) {
        this.lifecycleService = lifecycleService;
        this.replayService = replayService;
        this.backfillRunner = backfillRunner;
        this.clock = clock;
    }

    /**
     * Measures the squad without committing to anything.
     *
     * @return the calibration a campaign opened today would be given
     */
    @GetMapping("/calibration")
    @Operation(
        summary = "Preview the squad's calibration",
        description = """
            Measures the active roster exactly as opening a campaign would, and commits to nothing.

            A calibration is decided once and never revised, so this is the only moment to notice
            that a player's history is thin, or that the nine-month window had to shrink to cover
            everyone. Run the history backfill first if the window looks shorter than it should.
            """
    )
    @ApiResponse(responseCode = "200", description = "Calibration computed successfully.")
    public SquadCalibrationResponse previewCalibration() {
        SquadCalibration calibration = lifecycleService.previewCalibration();

        return new SquadCalibrationResponse(
            calibration.reference(),
            calibration.tier(),
            calibration.scaling().volumeFactor(),
            calibration.windowMonths(),
            calibration.firstDay(),
            calibration.players()
        );
    }

    /**
     * Walks the calibration window out of Henrik, in the background.
     */
    @PostMapping("/backfill")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
        summary = "Import the calibration window",
        description = """
            Walks each active operator's Henrik history back nine months, so a calibration is
            measured on the whole window rather than on the two acts the ordinary synchronization
            keeps.

            Thousands of calls under Henrik's rate limit: the request is acknowledged immediately
            and the walk is watched in the synchronization history.
            """
    )
    @ApiResponse(responseCode = "202", description = "Backfill accepted and running in the background.")
    public void backfillHistory() {
        backfillRunner.run();
    }

    /**
     * Opens a campaign starting the Monday after today.
     *
     * @return the campaign that was opened
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Open a campaign",
        description = """
            Freezes the active roster, measures the squad and draws the ten weeks with their
            guardians. The campaign starts the following Monday and runs for ten weeks.

            Refused while another campaign is opened or running, and refused on an empty roster.
            """
    )
    @ApiResponse(responseCode = "201", description = "Campaign opened successfully.")
    @ApiResponse(responseCode = "409", description = "A campaign is already live, or no operator is active.")
    public CampaignAdminResponse openCampaign() {
        return toResponse(lifecycleService.open());
    }

    /**
     * Stops the live campaign, freezing it at yesterday's base.
     *
     * @return the campaign that was stopped
     */
    @PostMapping("/stop")
    @Operation(
        summary = "Stop the live campaign",
        description = """
            Closes the campaign early and freezes it at the last day that is actually over. Its
            score stops there and is never recomputed.
            """
    )
    @ApiResponse(responseCode = "200", description = "Campaign stopped successfully.")
    @ApiResponse(responseCode = "409", description = "No campaign is opened or running.")
    public CampaignAdminResponse stopCampaign() {
        return toResponse(lifecycleService.stop(clock));
    }

    /**
     * Replays the campaign in progress from its first day.
     */
    @PostMapping("/replay")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Replay the campaign in progress",
        description = """
            Rebuilds every day and every settled week of the running campaign from the matches and
            challenges it is made of.

            A repair tool: the synchronization and the nightly tick already replay it, and the
            replay is idempotent, so running it by hand can only ever produce the same rows.
            """
    )
    @ApiResponse(responseCode = "204", description = "Campaign replayed successfully.")
    public void replayCampaign() {
        replayService.replayRunningCampaign();
    }

    /**
     * Deletes one campaign and everything it owns.
     *
     * @param id campaign identifier
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Delete a campaign",
        description = """
            Removes a campaign along with its roster, its weeks and every day it stored. Matches,
            challenges and rankings are untouched: the campaign is derived from them, never the
            other way round.
            """
    )
    @ApiResponse(responseCode = "204", description = "Campaign deleted successfully.")
    @ApiResponse(responseCode = "404", description = "No campaign owns the identifier.")
    public void deleteCampaign(@PathVariable long id) {
        lifecycleService.delete(id);
    }

    /**
     * Maps one campaign to the backoffice's answer.
     *
     * @param campaign campaign to map
     * @return the response
     */
    private CampaignAdminResponse toResponse(Campaign campaign) {
        return new CampaignAdminResponse(
            campaign.getId(),
            campaign.getNumber(),
            campaign.getStatus(),
            campaign.getFirstWeekStart(),
            campaign.getLastWeekStart(),
            campaign.getStoppedOn(),
            campaign.getReference(),
            campaign.getTier(),
            campaign.getRosterSize()
        );
    }
}
