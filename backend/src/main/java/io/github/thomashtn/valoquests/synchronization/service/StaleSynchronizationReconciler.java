package io.github.thomashtn.valoquests.synchronization.service;

import io.github.thomashtn.valoquests.synchronization.entity.Synchronization;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationRepository;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Closes synchronization executions that a shutdown interrupted.
 *
 * <p>Single instance only: a {@code RUNNING} row found at startup is dead, else it would show as
 * in progress forever.
 */
@Component
public class StaleSynchronizationReconciler implements ApplicationRunner {

    /**
     * Application logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(StaleSynchronizationReconciler.class);

    /**
     * Message stored on the executions this reconciler closes.
     */
    private static final String INTERRUPTION_MESSAGE =
        "Interrupted by an application restart. The matches imported before the interruption are "
            + "kept; the next synchronization resumes from there.";

    /**
     * Repository used to find and close interrupted executions.
     */
    private final SynchronizationRepository synchronizationRepository;

    /**
     * Application clock.
     */
    private final Clock clock;

    /**
     * Creates the stale synchronization reconciler.
     *
     * @param synchronizationRepository synchronization repository
     * @param clock                     application clock
     */
    public StaleSynchronizationReconciler(
        SynchronizationRepository synchronizationRepository,
        Clock clock
    ) {
        this.synchronizationRepository = synchronizationRepository;
        this.clock = clock;
    }

    /**
     * Marks every interrupted execution as failed.
     *
     * @param args application arguments, unused
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Synchronization> interrupted =
            synchronizationRepository.findAllByStatusIn(SynchronizationStatus.IN_PROGRESS);

        if (interrupted.isEmpty()) {
            return;
        }

        interrupted.forEach(synchronization -> {
            synchronization.setStatus(SynchronizationStatus.FAILED);
            synchronization.setFinishedAt(clock.instant());
            synchronization.setErrorMessage(INTERRUPTION_MESSAGE);
        });

        synchronizationRepository.saveAll(interrupted);

        LOGGER.warn(
            "Closed {} synchronization execution(s) left in progress by a previous shutdown",
            interrupted.size()
        );
    }
}
