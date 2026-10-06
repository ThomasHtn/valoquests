package io.github.thomashtn.valoquests.campaign.scheduler;

import io.github.thomashtn.valoquests.campaign.service.DailyTickService;
import io.github.thomashtn.valoquests.shared.concurrency.MatchHistoryLock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Closes the day just past and opens the new one.
 *
 * <p>Fires in {@code app.calendar-zone} (the {@code WeekCalendar} zone) once {@link MatchHistoryLock} is
 * free. Never closes a finished campaign: Monday's tick runs before the rollover imports Sunday's matches.
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
     * Longest wait for a running job, generous enough for a full manual synchronization.
     */
    private static final Duration LOCK_WAIT = Duration.ofHours(2);

    /**
     * Service running the tick, shared with the administrative route that triggers it by hand.
     */
    private final DailyTickService dailyTickService;

    /**
     * Lock keeping the tick from overlapping another job writing the match history.
     */
    private final MatchHistoryLock matchHistoryLock;

    /**
     * Creates the campaign daily tick scheduler.
     *
     * @param dailyTickService daily tick service
     * @param matchHistoryLock lock shared by every job writing the match history
     */
    public CampaignDailyTickScheduler(
        DailyTickService dailyTickService,
        MatchHistoryLock matchHistoryLock
    ) {
        this.dailyTickService = dailyTickService;
        this.matchHistoryLock = matchHistoryLock;
    }

    /**
     * Runs the tick once the lock is free, swallowing failures so the schedule survives.
     */
    @Scheduled(
        cron = "${app.scheduling.campaign-tick-cron}",
        zone = "${app.calendar-zone}"
    )
    public void tick() {
        try {
            if (!matchHistoryLock.runWhenFree(this::runTick, LOCK_WAIT)) {
                LOGGER.error(
                    "Scheduled campaign tick skipped: another job writing the match history still ran "
                        + "after {}. Run POST /api/admin/campaigns/tick once it has finished.",
                    LOCK_WAIT
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.error(
                "Scheduled campaign tick interrupted while waiting for another job. "
                    + "Run POST /api/admin/campaigns/tick."
            );
        }
    }

    /**
     * Runs the tick, logging any failure.
     */
    private void runTick() {
        LOGGER.info("Scheduled campaign tick started");

        try {
            dailyTickService.run();
            LOGGER.info("Scheduled campaign tick completed");
        } catch (RuntimeException exception) {
            LOGGER.error("Scheduled campaign tick failed unexpectedly", exception);
        }
    }
}
