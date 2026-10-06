package io.github.thomashtn.valoquests.week.scheduler;

import io.github.thomashtn.valoquests.shared.concurrency.MatchHistoryLock;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valoquests.synchronization.service.SynchronizationCommandService;
import io.github.thomashtn.valoquests.week.service.WeeklyRolloverService;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Automatically finalizes the previous week and prepares the new one.
 *
 * <p>Synchronizes first and fires two hours after midnight, so late Sunday matches are in before the week
 * freezes. Waits for the {@link MatchHistoryLock}, since a skipped rollover would leave the week open.
 */
@Component
@ConditionalOnProperty(
    name = "app.scheduling.week-rollover-enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class WeeklyRolloverScheduler {

    /**
     * Application logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(
            WeeklyRolloverScheduler.class
        );

    /**
     * Longest wait for a running job, generous enough for a full manual synchronization.
     */
    private static final Duration LOCK_WAIT = Duration.ofHours(2);

    /**
     * Service executing the transactional weekly rollover.
     */
    private final WeeklyRolloverService

        weeklyRolloverService;

    /**
     * Service used to import the matches played since the last synchronization.
     */
    private final SynchronizationCommandService

        synchronizationCommandService;

    /**
     * Lock keeping the rollover and its synchronization from overlapping another guarded job.
     */
    private final MatchHistoryLock matchHistoryLock;

    /**
     * Creates the weekly rollover scheduler.
     *
     * @param weeklyRolloverService        weekly rollover service
     * @param synchronizationCommandService synchronization command service
     * @param matchHistoryLock             lock shared by every job writing the match history
     */
    public WeeklyRolloverScheduler(
        WeeklyRolloverService weeklyRolloverService,
        SynchronizationCommandService synchronizationCommandService,
        MatchHistoryLock matchHistoryLock
    ) {
        this.weeklyRolloverService =
            weeklyRolloverService;

        this.synchronizationCommandService =
            synchronizationCommandService;

        this.matchHistoryLock = matchHistoryLock;
    }

    /**
     * Executes the rollover every Monday, at the configured hour of the calendar zone.
     *
     * <p>A failed or skipped rollover is caught up by {@link MissedScheduleCatchUp} at the next startup,
     * or by {@code POST /api/admin/weeks/rollover}.
     */
    @Scheduled(
        cron = "${app.scheduling.week-rollover-cron}",
        zone = "${app.calendar-zone}"
    )
    public void rolloverWeek() {
        try {
            if (!matchHistoryLock.runWhenFree(this::synchronizeThenRollover, LOCK_WAIT)) {
                LOGGER.error(
                    "Scheduled weekly rollover skipped: another synchronization, rollover or reset "
                        + "still ran after {}. Run POST /api/admin/weeks/rollover once it has finished.",
                    LOCK_WAIT
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.error(
                "Scheduled weekly rollover interrupted while waiting for another job. "
                    + "Run POST /api/admin/weeks/rollover."
            );
        }
    }

    /**
     * Imports the last matches, then finalizes the previous week and opens the new one.
     */
    private void synchronizeThenRollover() {
        LOGGER.info("Scheduled weekly rollover started");

        importMatchesPlayedSinceLastSynchronization();

        try {
            weeklyRolloverService.rolloverIfNeeded();
            LOGGER.info("Scheduled weekly rollover completed");
        } catch (RuntimeException exception) {
            LOGGER.error(
                "Scheduled weekly rollover failed unexpectedly",
                exception
            );
        }
    }

    /**
     * Imports the matches played between the last synchronization and the rollover.
     *
     * <p>Runs outside the rollover transaction so imported matches survive a rollover failure. A failure
     * is logged and the rollover proceeds: missing a few matches beats leaving the week open.
     */
    private void importMatchesPlayedSinceLastSynchronization() {
        try {
            synchronizationCommandService.synchronizeAllPlayers(
                SynchronizationTrigger.SCHEDULED
            );
        } catch (RuntimeException exception) {
            LOGGER.error(
                "Pre-rollover synchronization failed. The closing week is finalized without the "
                    + "matches played since the last successful synchronization.",
                exception
            );
        }
    }
}
