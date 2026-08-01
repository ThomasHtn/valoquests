package io.github.thomashtn.valorant.tracker.synchronization.repository;

import io.github.thomashtn.valorant.tracker.synchronization.entity.Synchronization;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for synchronization executions.
 */
public interface SynchronizationRepository
    extends JpaRepository<Synchronization, Long> {

    /**
     * Returns the most recently started synchronization execution.
     *
     * @return the latest execution, or empty when none has ever run
     */
    Optional<Synchronization> findFirstByOrderByStartedAtDescIdDesc();
}
