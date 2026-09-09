package io.github.thomashtn.valoquests.campaign.scheduler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.service.DailyTickService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Closes the day just past and opens the new one.
 *
 * <p>Three things, in order: the day's challenge is drawn so the squad wakes up with one, a campaign
 * whose first Monday has come starts, and the whole campaign is replayed so the evening's meal and
 * yesterday's Sunday settlement are written down. A finished campaign is not closed here: on a
 * Monday the tick fires before the rollover has imported the last Sunday matches, and a campaign
 * closed at 00:10 would freeze its final score without them.
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
     * Service running the tick, shared with the administrative route that triggers it by hand.
     */
    private final DailyTickService dailyTickService;

    /**
     * Creates the campaign daily tick scheduler.
     *
     * @param dailyTickService daily tick service
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public CampaignDailyTickScheduler(DailyTickService dailyTickService) {
        this.dailyTickService = dailyTickService;
    }

    /**
     * Runs the tick, swallowing any failure so a bad night never kills the schedule.
     */
    @Scheduled(
        cron = "${app.scheduling.campaign-tick-cron}",
        zone = "${app.scheduling.week-rollover-zone}"
    )
    public void tick() {
        LOGGER.info("Scheduled campaign tick started");

        try {
            dailyTickService.run();
            LOGGER.info("Scheduled campaign tick completed");
        } catch (RuntimeException exception) {
            LOGGER.error("Scheduled campaign tick failed unexpectedly", exception);
        }
    }
}
