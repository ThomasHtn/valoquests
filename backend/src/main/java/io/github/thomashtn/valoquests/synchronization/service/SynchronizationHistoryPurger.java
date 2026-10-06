package io.github.thomashtn.valoquests.synchronization.service;

import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationPlayerResultRepository;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes old quiet executions: completed passes that imported nothing.
 *
 * <p>Executions that imported matches or failed are kept for diagnosis.
 */
@Service
public class SynchronizationHistoryPurger {

    /**
     * Logger used to report how many executions were deleted.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizationHistoryPurger.class);

    /**
     * Repository of synchronization executions.
     */
    private final SynchronizationRepository synchronizationRepository;

    /**
     * Repository of per-player outcomes, deleted before their execution.
     */
    private final SynchronizationPlayerResultRepository playerResultRepository;

    /**
     * Clock used to compute the retention cutoff.
     */
    private final Clock clock;

    /**
     * How long a quiet execution stays in the history.
     */
    private final Duration quietRetention;

    /**
     * Creates the history purger.
     *
     * @param synchronizationRepository repository of synchronization executions
     * @param playerResultRepository    repository of per-player outcomes
     * @param clock                     application clock
     * @param quietRetention            how long a quiet execution stays in the history
     */
    public SynchronizationHistoryPurger(
        SynchronizationRepository synchronizationRepository,
        SynchronizationPlayerResultRepository playerResultRepository,
        Clock clock,
        @Value("${app.synchronization.quiet-history-retention}") Duration quietRetention
    ) {
        this.synchronizationRepository = synchronizationRepository;
        this.playerResultRepository = playerResultRepository;
        this.clock = clock;
        this.quietRetention = quietRetention;
    }

    /**
     * Deletes the quiet executions that finished before the retention window, with their outcomes.
     *
     * @return number of executions deleted
     */
    @Transactional
    public int purgeQuietExecutions() {
        Instant cutoff = clock.instant().minus(quietRetention);
        playerResultRepository.purgeQuietExecutionResults(SynchronizationStatus.COMPLETED, cutoff);
        int deleted = synchronizationRepository.purgeQuietExecutions(SynchronizationStatus.COMPLETED, cutoff);

        LOGGER.info("Deleted {} quiet synchronization execution(s) finished before {}", deleted, cutoff);
        return deleted;
    }
}
