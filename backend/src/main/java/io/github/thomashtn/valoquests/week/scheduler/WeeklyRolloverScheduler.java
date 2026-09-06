package io.github.thomashtn.valoquests.week.scheduler;

import io.github.thomashtn.valoquests.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valoquests.synchronization.service.SynchronizationCommandService;
import io.github.thomashtn.valoquests.week.service.WeeklyRolloverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Automatically finalizes the previous week and prepares the new one.
 *
 * <p>A synchronization runs first, and the job fires two hours after midnight rather than at it.
 * Henrik only returns finished matches, and a competitive game started late on Sunday ends well
 * after 00:00: frozen at 00:05, the week lost those matches for its ranking and its challenges,
 * which no later run ever revisits, while the campaign, replayed whole, still counted them.
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
     * Creates the weekly rollover scheduler.
     *
     * @param weeklyRolloverService        weekly rollover service
     * @param synchronizationCommandService synchronization command service
     */
    public WeeklyRolloverScheduler(
        WeeklyRolloverService weeklyRolloverService,
        SynchronizationCommandService synchronizationCommandService
    ) {
        this.weeklyRolloverService =
            weeklyRolloverService;

        this.synchronizationCommandService =
            synchronizationCommandService;
    }

    /**
     * Executes the rollover every Monday, at the configured hour of the week zone.
     *
     * <p>Errors are logged and allowed to be retried during the next
     * execution. The transactional service prevents partial finalization.</p>
     */
    @Scheduled(
        cron = "${app.scheduling.week-rollover-cron}",
        zone = "${app.scheduling.week-rollover-zone}"
    )
    public void rolloverWeek() {
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
     * <p>Runs outside the rollover transaction, which is what the synchronization requires and what
     * lets its imported matches survive a later rollover failure.
     *
     * <p>A failure is logged and the rollover proceeds. Finalizing a week that may miss its very
     * last matches is the lesser evil: skipping the rollover would leave the week open forever,
     * since the next run only ever looks at the week that just ended.
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
