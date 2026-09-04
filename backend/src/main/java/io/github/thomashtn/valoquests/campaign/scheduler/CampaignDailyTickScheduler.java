package io.github.thomashtn.valoquests.campaign.scheduler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.service.CampaignLifecycleService;
import io.github.thomashtn.valoquests.campaign.service.CampaignReplayService;
import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.challenge.service.WeeklyChallengeSelectionService;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Closes the day just past and opens the new one.
 *
 * <p>Four things, in order: the day's challenge is drawn so the squad wakes up with one, a campaign
 * whose first Monday has come starts, the whole campaign is replayed so the evening's meal and the
 * Sunday settlement are written down, and only then is a campaign past its tenth Sunday closed —
 * closing first would freeze a score one settlement short.
 *
 * <p>Scheduled in {@code app.scheduling.week-rollover-zone}, the zone {@code WeekCalendar} splits
 * days on. That is not cosmetic: the calendar decides which day a match counts towards, so a tick
 * firing in another zone would close a day whose boundaries are not the ones that produced its
 * gains.
 *
 * <p>Every step is idempotent, and the synchronization replays the campaign too. This is the safety
 * net that makes the midnight boundary real even on a night nobody plays.
 */
@Component
@ConditionalOnProperty(
    name = "app.scheduling.campaign-tick-enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class CampaignDailyTickScheduler {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignDailyTickScheduler.class);

    /**
     * Service drawing the day's challenge.
     */
    private final WeeklyChallengeSelectionService selectionService;

    /**
     * Service giving the day's challenge its progress rows, and the ranking its points.
     */
    private final ChallengeRecalculationService recalculationService;

    /**
     * Service starting and closing campaigns.
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
     * Clock stamping a campaign's closing instant.
     */
    private final Clock clock;

    /**
     * Creates the campaign daily tick scheduler.
     *
     * @param selectionService     challenge selection service
     * @param recalculationService challenge recalculation service
     * @param lifecycleService     campaign lifecycle service
     * @param replayService        campaign replay service
     * @param weekCalendar         week calendar
     * @param clock                clock
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public CampaignDailyTickScheduler(
        WeeklyChallengeSelectionService selectionService,
        ChallengeRecalculationService recalculationService,
        CampaignLifecycleService lifecycleService,
        CampaignReplayService replayService,
        WeekCalendar weekCalendar,
        Clock clock
    ) {
        this.selectionService = selectionService;
        this.recalculationService = recalculationService;
        this.lifecycleService = lifecycleService;
        this.replayService = replayService;
        this.weekCalendar = weekCalendar;
        this.clock = clock;
    }

    /**
     * Draws the day's challenge, evaluates it, advances the campaign's lifecycle and replays it.
     *
     * <p>The recalculation sits between the draw and the replay: the challenge drawn a second ago
     * has no progress row until it runs, and the replay reads those rows for the week's rescues.
     */
    @Scheduled(
        cron = "${app.scheduling.campaign-tick-cron}",
        zone = "${app.scheduling.week-rollover-zone}"
    )
    public void tick() {
        LOGGER.info("Scheduled campaign tick started");

        try {
            selectionService.selectDailyChallenge(weekCalendar.today());
            recalculationService.recalculateCurrentWeekProgress();
            lifecycleService.startIfDue();
            replayService.replayRunningCampaign();
            lifecycleService.closeIfComplete(clock);
            LOGGER.info("Scheduled campaign tick completed");
        } catch (RuntimeException exception) {
            LOGGER.error("Scheduled campaign tick failed unexpectedly", exception);
        }
    }
}
