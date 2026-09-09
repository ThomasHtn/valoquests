package io.github.thomashtn.valoquests.campaign.controller;

import static io.github.thomashtn.valoquests.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.dto.CampaignAdminResponse;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.model.CampaignStartWeek;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.service.CampaignLifecycleService;
import io.github.thomashtn.valoquests.campaign.service.CampaignReplayService;
import io.github.thomashtn.valoquests.campaign.service.DailyTickService;
import io.github.thomashtn.valoquests.challenge.model.CampaignDifficulty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the protected campaign lifecycle: open, stop, tick, delete.
 *
 * <p>Nothing here happens on its own. A campaign is opened by a person who picks its difficulty,
 * and the ten weeks that follow are decided in that single moment.
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
     * Service running the daily tick by hand.
     */
    private final DailyTickService dailyTickService;

    /**
     * Clock stamping the closing instant.
     */
    private final Clock clock;

    /**
     * Creates the campaign administration controller.
     *
     * @param lifecycleService campaign lifecycle service
     * @param replayService    campaign replay service
     * @param dailyTickService daily tick service
     * @param clock            clock
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public CampaignAdminController(
        CampaignLifecycleService lifecycleService,
        CampaignReplayService replayService,
        DailyTickService dailyTickService,
        Clock clock
    ) {
        this.lifecycleService = lifecycleService;
        this.replayService = replayService;
        this.dailyTickService = dailyTickService;
        this.clock = clock;
    }

    /**
     * Opens a campaign on the chosen Monday.
     *
     * @param difficulty difficulty the campaign is played at
     * @param startWeek  week the campaign starts on
     * @return the campaign that was opened
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Open a campaign",
        description = """
            Freezes the active roster, measures the squad and draws the ten weeks with their
            guardians.

            The start week decides the first Monday: NEXT_WEEK begins on a week nobody has played
            yet, CURRENT_WEEK begins on the Monday of the week in progress, so the days already
            played count. A campaign opened on the current week is running immediately and is
            replayed on the spot to rebuild those days.

            Refused while another campaign is opened or running, and refused on an empty roster.

            The difficulty is the single dial: it carries the reference the guardians, the groups
            and the challenge rewards are multiples of, and decides which of the two grids written
            in the catalogue the campaign's challenges are drawn from. It is frozen at opening.
            """
    )
    @ApiResponse(responseCode = "201", description = "Campaign opened successfully.")
    @ApiResponse(responseCode = "409", description = "A campaign is already live, or no operator is active.")
    public CampaignAdminResponse openCampaign(
        @RequestParam(defaultValue = "AMATEUR") CampaignDifficulty difficulty,
        @RequestParam(defaultValue = "NEXT_WEEK") CampaignStartWeek startWeek
    ) {
        Campaign campaign = lifecycleService.open(difficulty, startWeek);

        if (campaign.getStatus() == CampaignStatus.RUNNING) {
            replayService.replay(campaign);
        }

        return toResponse(campaign);
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
     * Runs the daily tick now, instead of waiting for the next midnight.
     */
    @PostMapping("/tick")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Run the daily tick now",
        description = """
            Runs the exact sequence the nightly tick runs: it draws the day's challenge if the day
            has none, rebuilds the week's challenge progress and its ranking, starts a campaign
            whose first Monday has come, and replays the campaign from its first day.

            A repair tool for a tick that did not run, and the single command behind the three it
            replaces: the synchronization already runs the recalculation and the replay whenever it
            imports a match. Every step is idempotent, so running it by hand can only ever produce
            the same rows.
            """
    )
    @ApiResponse(responseCode = "204", description = "The tick completed.")
    public void runDailyTick() {
        dailyTickService.run();
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
            campaign.reference(),
            campaign.getDifficulty(),
            campaign.getRosterSize()
        );
    }
}
