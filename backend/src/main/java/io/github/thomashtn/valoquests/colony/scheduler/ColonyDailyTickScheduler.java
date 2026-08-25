package io.github.thomashtn.valoquests.colony.scheduler;

import io.github.thomashtn.valoquests.colony.service.ColonyReplayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Advances the colony once a day.
 *
 * <p>Scheduled in {@code app.scheduling.week-rollover-zone}, the same zone {@code WeekCalendar} reads.
 * That is not cosmetic: the calendar decides which day a match counts towards, so a tick firing in a
 * different zone would replay days whose boundaries do not match the ones producing the gains.
 *
 * <p>Fires ten minutes after the weekly rollover, so a Monday's tick already sees the week that has just
 * been finalized and the materials it credits. The replay is idempotent, so an extra firing costs
 * nothing, and this is only ever a safety net: every synchronization replays the colony too, which is
 * what makes a day's gains visible on the day itself.
 */
@Component
@ConditionalOnProperty(
    name = "app.scheduling.colony-tick-enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ColonyDailyTickScheduler {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ColonyDailyTickScheduler.class);

    /**
     * Service replaying the run in progress.
     */
    private final ColonyReplayService replayService;

    /**
     * Creates the colony daily tick scheduler.
     *
     * @param replayService colony replay service
     */
    public ColonyDailyTickScheduler(ColonyReplayService replayService) {
        this.replayService = replayService;
    }

    /**
     * Replays the run in progress.
     */
    @Scheduled(
        cron = "${app.scheduling.colony-tick-cron}",
        zone = "${app.scheduling.week-rollover-zone}"
    )
    public void tick() {
        LOGGER.info("Scheduled colony tick started");
        try {
            replayService.replayCurrentRun();
            LOGGER.info("Scheduled colony tick completed");
        } catch (RuntimeException exception) {
            LOGGER.error("Scheduled colony tick failed unexpectedly", exception);
        }
    }
}
