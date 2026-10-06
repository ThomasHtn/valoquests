package io.github.thomashtn.valoquests.synchronization.scheduler;

import io.github.thomashtn.valoquests.shared.concurrency.MatchHistoryLock;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valoquests.synchronization.service.SynchronizationCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically synchronizes recent Valorant data for every active player.
 *
 * <p>The scheduler delegates the complete workflow to the same command service
 * used by administrative routes. Scheduled executions are therefore persisted
 * with the {@link SynchronizationTrigger#SCHEDULED} trigger and benefit from
 * the existing per-player failure isolation.</p>
 *
 * <p>A run is skipped, not queued, while another guarded job holds the {@link MatchHistoryLock}:
 * the next one comes thirty minutes later anyway.</p>
 */
@Component
@ConditionalOnProperty(
    prefix = "app.scheduling",
    name = "standard-synchronization-enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class StandardSynchronizationScheduler {

    /**
     * Logger used to expose scheduler lifecycle events.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(StandardSynchronizationScheduler.class);

    /**
     * Command service executing and recording the synchronization workflow.
     */
    private final SynchronizationCommandService synchronizationCommandService;

    /**
     * Lock keeping this run from overlapping another synchronization, rollover or reset.
     */
    private final MatchHistoryLock matchHistoryLock;

    /**
     * Creates the standard synchronization scheduler.
     *
     * @param synchronizationCommandService synchronization orchestration service
     * @param matchHistoryLock              lock shared by every job writing the match history
     */
    public StandardSynchronizationScheduler(
        SynchronizationCommandService synchronizationCommandService,
        MatchHistoryLock matchHistoryLock
    ) {
        this.synchronizationCommandService = synchronizationCommandService;
        this.matchHistoryLock = matchHistoryLock;
    }

    /**
     * Runs the configured standard synchronization job.
     *
     * <p>Unexpected runtime failures are logged instead of escaping the scheduled
     * method so a temporary failure does not disable later executions.</p>
     */
    @Scheduled(
        cron = "${app.scheduling.standard-synchronization-cron}",
        zone = "${app.calendar-zone}"
    )
    public void synchronizeAllActivePlayers() {
        if (!matchHistoryLock.runIfFree(this::synchronize)) {
            LOGGER.info(
                "Scheduled standard synchronization skipped: another synchronization, rollover or "
                    + "reset is running"
            );
        }
    }

    /**
     * Synchronizes every player, logging a failure instead of propagating it.
     */
    private void synchronize() {
        LOGGER.info("Starting scheduled standard synchronization");

        try {
            synchronizationCommandService.synchronizeAllPlayers(
                SynchronizationTrigger.SCHEDULED
            );
            LOGGER.info("Scheduled standard synchronization completed");
        } catch (RuntimeException exception) {
            LOGGER.error(
                "Scheduled standard synchronization failed unexpectedly",
                exception
            );
        }
    }
}
