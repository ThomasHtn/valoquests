package io.github.thomashtn.valoquests.synchronization.scheduler;

import io.github.thomashtn.valoquests.synchronization.service.SynchronizationHistoryPurger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Trims the synchronization history once a day.
 *
 * <p>Needs no {@code MatchHistoryLock}: it only deletes finished executions, never a running one.
 */
@Component
@ConditionalOnProperty(
    name = "app.scheduling.synchronization-history-purge-enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SynchronizationHistoryPurgeScheduler {

    /**
     * Logger used to report an unexpected failure.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(SynchronizationHistoryPurgeScheduler.class);

    /**
     * Service deleting the old quiet executions.
     */
    private final SynchronizationHistoryPurger historyPurger;

    /**
     * Creates the purge scheduler.
     *
     * @param historyPurger service deleting the old quiet executions
     */
    public SynchronizationHistoryPurgeScheduler(SynchronizationHistoryPurger historyPurger) {
        this.historyPurger = historyPurger;
    }

    /**
     * Runs the purge, logging a failure so later executions stay scheduled.
     */
    @Scheduled(
        cron = "${app.scheduling.synchronization-history-purge-cron}",
        zone = "${app.calendar-zone}"
    )
    public void purge() {
        try {
            historyPurger.purgeQuietExecutions();
        } catch (RuntimeException exception) {
            LOGGER.error("Scheduled synchronization history purge failed", exception);
        }
    }
}
