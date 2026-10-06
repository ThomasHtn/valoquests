package io.github.thomashtn.valoquests.week.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.service.CampaignLifecycleService;
import io.github.thomashtn.valoquests.campaign.service.CampaignReplayService;
import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.challenge.service.DailyChallengeDrawService;
import io.github.thomashtn.valoquests.challenge.service.WeeklyChallengeDrawService;
import io.github.thomashtn.valoquests.ranking.service.RankingRecalculationService;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Opens the current week: settles the campaign up to it, draws its challenges and ranks it at zero.
 *
 * <p>A campaign week is settled by the replay, which the rollover, the nightly tick and every
 * synchronization all run: nothing has to be closed once and only once, so nothing can be missed.
 */
@Component
public class WeekOpener {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WeekOpener.class);

    /**
     * Service starting a campaign whose first Monday has come, and closing a finished one.
     */
    private final CampaignLifecycleService campaignLifecycleService;

    /**
     * Service settling the week that has just ended.
     */
    private final CampaignReplayService campaignReplayService;

    /**
     * Service drawing the new week's challenge pack.
     */
    private final WeeklyChallengeDrawService weeklyDrawService;

    /**
     * Service drawing the new week's first daily challenge.
     */
    private final DailyChallengeDrawService dailyDrawService;

    /**
     * Service giving the new week's challenges their progress rows.
     */
    private final ChallengeRecalculationService challengeRecalculationService;

    /**
     * Service giving the new week its ranking rows.
     */
    private final RankingRecalculationService rankingRecalculationService;

    /**
     * Creates the week opener.
     *
     * @param campaignLifecycleService      campaign lifecycle service
     * @param campaignReplayService         campaign replay service
     * @param weeklyDrawService             weekly challenge draw service
     * @param dailyDrawService              daily challenge draw service
     * @param challengeRecalculationService challenge progress recalculation service
     * @param rankingRecalculationService   ranking recalculation service
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public WeekOpener(
        CampaignLifecycleService campaignLifecycleService,
        CampaignReplayService campaignReplayService,
        WeeklyChallengeDrawService weeklyDrawService,
        DailyChallengeDrawService dailyDrawService,
        ChallengeRecalculationService challengeRecalculationService,
        RankingRecalculationService rankingRecalculationService
    ) {
        this.campaignLifecycleService = campaignLifecycleService;
        this.campaignReplayService = campaignReplayService;
        this.weeklyDrawService = weeklyDrawService;
        this.dailyDrawService = dailyDrawService;
        this.challengeRecalculationService = challengeRecalculationService;
        this.rankingRecalculationService = rankingRecalculationService;
    }

    /**
     * Settles the week that has just ended, then opens the new one.
     *
     * <p>The replay comes first: the Monday being opened is the day after a Sunday that has to be
     * settled, and drawing the new pack before settling it would credit the new week's challenges
     * to the old week's ship.
     *
     * <p>The close comes right after: a campaign whose tenth Sunday has just been settled is over,
     * and closing it here rather than at the nightly tick means the Monday after it never shows a
     * running campaign with nothing left to run.
     *
     * <p>Idempotent throughout, and it catches up on its own: a rollover firing after a long outage
     * replays every week it missed in the one pass, because the replay never reads a stored total.
     *
     * @param weekStart Monday identifying the new week
     */
    public void openWeek(LocalDate weekStart) {
        campaignLifecycleService.startIfDue();
        campaignReplayService.replayRunningCampaign();
        campaignLifecycleService.closeIfComplete();
        weeklyDrawService.selectWeekChallenges(weekStart);
        dailyDrawService.selectDailyChallenge(weekStart);

        LOGGER.info("Week {} opened: challenge pack and daily challenge drawn.", weekStart);

        // Without these rows the podium would show its empty state until the next synchronization.
        challengeRecalculationService.recalculateWeekProgress(weekStart);
        rankingRecalculationService.recalculateWeek(weekStart);
    }
}
