package io.github.thomashtn.valoquests.campaign.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.challenge.service.DailyChallengeDrawService;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import org.springframework.stereotype.Service;

/**
 * Runs the daily tick: the day's challenge, its progress, a due campaign started and replayed.
 *
 * <p>One sequence for the scheduler and the backoffice, so an admin never chains partial commands.
 */
@Service
public class DefaultDailyTickService implements DailyTickService {

    /**
     * Service drawing the day's challenge.
     */
    private final DailyChallengeDrawService dailyDrawService;

    /**
     * Service giving the day's challenge its progress rows, and the ranking its points.
     */
    private final ChallengeRecalculationService recalculationService;

    /**
     * Service starting campaigns whose first Monday has come.
     */
    private final CampaignLifecycleService lifecycleService;

    /**
     * Service replaying the campaign in progress.
     */
    private final CampaignReplayService replayService;

    /**
     * Calendar resolving the day being opened.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the daily tick service.
     *
     * @param dailyDrawService     daily challenge draw service
     * @param recalculationService challenge recalculation service
     * @param lifecycleService     campaign lifecycle service
     * @param replayService        campaign replay service
     * @param weekCalendar         week calendar
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public DefaultDailyTickService(
        DailyChallengeDrawService dailyDrawService,
        ChallengeRecalculationService recalculationService,
        CampaignLifecycleService lifecycleService,
        CampaignReplayService replayService,
        WeekCalendar weekCalendar
    ) {
        this.dailyDrawService = dailyDrawService;
        this.recalculationService = recalculationService;
        this.lifecycleService = lifecycleService;
        this.replayService = replayService;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Draws, recalculates, starts and replays, in that order.
     *
     * <p>The recalculation must precede the replay, which reads its progress rows for the rescues.
     */
    @Override
    public void run() {
        dailyDrawService.selectDailyChallenge(weekCalendar.today());
        recalculationService.drawAndRecalculateCurrentWeek();
        lifecycleService.startIfDue();
        replayService.replayRunningCampaign();
    }
}
