package io.github.thomashtn.valoquests.campaign.controller;

import static io.github.thomashtn.valoquests.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.dto.CampaignAdminResponse;
import io.github.thomashtn.valoquests.campaign.model.CampaignStartWeek;
import io.github.thomashtn.valoquests.campaign.service.CampaignLifecycleService;
import io.github.thomashtn.valoquests.campaign.service.DailyTickService;
import io.github.thomashtn.valoquests.scoring.model.CampaignDifficulty;
import io.github.thomashtn.valoquests.shared.concurrency.MatchHistoryLock;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Administration - Campaigns", description = "Campaign lifecycle and daily tick.")
@SecurityRequirement(name = ADMIN_KEY_SECURITY_SCHEME)
public class CampaignAdminController {

    /**
     * Service opening, starting and stopping campaigns.
     */
    private final CampaignLifecycleService lifecycleService;

    /**
     * Service running the daily tick by hand.
     */
    private final DailyTickService dailyTickService;

    /**
     * Lock keeping the tick from overlapping another job writing the match history.
     */
    private final MatchHistoryLock matchHistoryLock;

    /**
     * Creates the campaign administration controller.
     *
     * @param lifecycleService campaign lifecycle service
     * @param dailyTickService daily tick service
     * @param matchHistoryLock lock shared by every job writing the match history
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public CampaignAdminController(
        CampaignLifecycleService lifecycleService,
        DailyTickService dailyTickService,
        MatchHistoryLock matchHistoryLock
    ) {
        this.lifecycleService = lifecycleService;
        this.dailyTickService = dailyTickService;
        this.matchHistoryLock = matchHistoryLock;
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
            Freezes the active roster and the difficulty, and draws the ten weeks with their
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
    @ApiResponse(responseCode = "409", description = "A campaign is already live, or no player is active.")
    public CampaignAdminResponse openCampaign(
        @RequestParam(defaultValue = "AMATEUR") CampaignDifficulty difficulty,
        @RequestParam(defaultValue = "NEXT_WEEK") CampaignStartWeek startWeek
    ) {
        return CampaignAdminResponse.from(lifecycleService.open(difficulty, startWeek));
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
        return CampaignAdminResponse.from(lifecycleService.stop());
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

            A repair tool for a tick that did not run; the synchronization already runs the
            recalculation and the replay whenever it imports a match. Every step is idempotent, so
            running it by hand can only ever produce the same rows.

            Refused with a 409 while another job writing the match history runs.
            """
    )
    @ApiResponse(responseCode = "204", description = "The tick completed.")
    @ApiResponse(
        responseCode = "409",
        description = "Another job writing the match history is already running."
    )
    public void runDailyTick() {
        // Taken outside the tick's transactions so it is only released once they committed.
        matchHistoryLock.runOrReject(dailyTickService::run);
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
}
