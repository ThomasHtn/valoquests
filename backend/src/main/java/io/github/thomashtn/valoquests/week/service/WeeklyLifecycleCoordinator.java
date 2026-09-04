package io.github.thomashtn.valoquests.week.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.service.CampaignLifecycleService;
import io.github.thomashtn.valoquests.campaign.service.CampaignReplayService;
import io.github.thomashtn.valoquests.challenge.service.WeeklyChallengeSelectionService;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opens a new week on behalf of {@link DefaultWeeklyRolloverService}.
 *
 * <p>Exists to keep that service's own constructor within this codebase's parameter limit: it
 * already sits at the practical maximum coordinating challenge and ranking recalculation, so the
 * campaign-side collaborators are grouped here instead of inflating it further.
 *
 * <p>There is no boss to close any more. A campaign week is settled by the replay, from the matches
 * and challenges of that week, every time the campaign is replayed — which the rollover, the
 * nightly tick and every synchronization all do. Nothing has to be closed once and only once, so
 * nothing can be missed once.
 */
@Service
public class WeeklyLifecycleCoordinator {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WeeklyLifecycleCoordinator.class);

    /**
     * Service drawing the new week's challenge pack and its first daily challenge.
     */
    private final WeeklyChallengeSelectionService weeklyChallengeSelectionService;

    /**
     * Service starting a campaign whose first Monday has come.
     */
    private final CampaignLifecycleService campaignLifecycleService;

    /**
     * Service settling the week that has just ended.
     */
    private final CampaignReplayService campaignReplayService;

    /**
     * Creates the weekly lifecycle coordinator.
     *
     * @param weeklyChallengeSelectionService challenge selection service
     * @param campaignLifecycleService        campaign lifecycle service
     * @param campaignReplayService           campaign replay service
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public WeeklyLifecycleCoordinator(
        WeeklyChallengeSelectionService weeklyChallengeSelectionService,
        CampaignLifecycleService campaignLifecycleService,
        CampaignReplayService campaignReplayService
    ) {
        this.weeklyChallengeSelectionService = weeklyChallengeSelectionService;
        this.campaignLifecycleService = campaignLifecycleService;
        this.campaignReplayService = campaignReplayService;
    }

    /**
     * Settles the week that has just ended, then opens the new one.
     *
     * <p>The replay comes first: the Monday being opened is the day after a Sunday that has to be
     * settled, and drawing the new pack before settling it would credit the new week's challenges
     * to the old week's ship.
     *
     * <p>Idempotent throughout, and it catches up on its own: a rollover firing after a long outage
     * replays every week it missed in the one pass, because the replay never reads a stored total.
     *
     * @param weekStart Monday identifying the new week
     */
    @Transactional
    public void openWeek(LocalDate weekStart) {
        campaignLifecycleService.startIfDue();
        campaignReplayService.replayRunningCampaign();
        weeklyChallengeSelectionService.selectWeekChallenges(weekStart);
        weeklyChallengeSelectionService.selectDailyChallenge(weekStart);

        LOGGER.info("Week {} opened: challenge pack and daily challenge drawn.", weekStart);
    }
}
