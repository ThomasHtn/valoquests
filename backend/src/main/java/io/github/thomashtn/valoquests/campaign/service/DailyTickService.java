package io.github.thomashtn.valoquests.campaign.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.challenge.service.WeeklyChallengeSelectionService;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import org.springframework.stereotype.Service;

/**
 * Runs the daily tick: the day's challenge, its progress, a due campaign started and replayed.
 *
 * <p>Extracted from the scheduler so the backoffice triggers the very same sequence rather than a
 * handful of partial commands an operator had to chain in the right order.
 */
@Service
public class DailyTickService {

    /**
     * Service drawing the day's challenge.
     */
    private final WeeklyChallengeSelectionService selectionService;

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
     * @param selectionService     challenge selection service
     * @param recalculationService challenge recalculation service
     * @param lifecycleService     campaign lifecycle service
     * @param replayService        campaign replay service
     * @param weekCalendar         week calendar
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public DailyTickService(
        WeeklyChallengeSelectionService selectionService,
        ChallengeRecalculationService recalculationService,
        CampaignLifecycleService lifecycleService,
        CampaignReplayService replayService,
        WeekCalendar weekCalendar
    ) {
        this.selectionService = selectionService;
        this.recalculationService = recalculationService;
        this.lifecycleService = lifecycleService;
        this.replayService = replayService;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Draws the day's challenge, evaluates it, starts a due campaign and replays it.
     *
     * <p>The recalculation sits between the draw and the replay: the challenge drawn a second ago
     * has no progress row until it runs, and the replay reads those rows for the week's rescues.
     *
     * <p>Every step is idempotent, so running it twice produces the very same rows.
     */
    public void run() {
        selectionService.selectDailyChallenge(weekCalendar.today());
        recalculationService.recalculateCurrentWeekProgress();
        lifecycleService.startIfDue();
        replayService.replayRunningCampaign();
    }
}
