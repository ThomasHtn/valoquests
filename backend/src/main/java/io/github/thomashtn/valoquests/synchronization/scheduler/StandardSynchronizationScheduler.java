package io.github.thomashtn.valoquests.synchronization.scheduler;

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
     * Creates the standard synchronization scheduler.
     *
     * @param synchronizationCommandService synchronization orchestration service
     */
    public StandardSynchronizationScheduler(
        SynchronizationCommandService synchronizationCommandService
    ) {
        this.synchronizationCommandService = synchronizationCommandService;
    }

    /**
     * Runs the configured standard synchronization job.
     *
     * <p>Unexpected runtime failures are logged instead of escaping the scheduled
     * method so a temporary failure does not disable later executions.</p>
     */
    @Scheduled(
        cron = "${app.scheduling.standard-synchronization-cron}",
        zone = "${app.scheduling.zone}"
    )
    public void synchronizeAllActivePlayers() {
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
