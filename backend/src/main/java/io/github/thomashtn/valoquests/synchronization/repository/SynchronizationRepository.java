package io.github.thomashtn.valoquests.synchronization.repository;

import io.github.thomashtn.valoquests.synchronization.entity.Synchronization;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStatus;
import java.util.Collection;
import java.util.List;
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

    /**
     * Determines whether an execution currently holds one of the supplied statuses.
     *
     * <p>This is what makes a synchronization request exclusive: a run is dispatched to a
     * background thread, so nothing else prevents a second request from starting a concurrent walk
     * of the same history and burning the Henrik rate limit twice.
     *
     * @param statuses statuses to look for
     * @return {@code true} when at least one execution holds one of them
     */
    boolean existsByStatusIn(Collection<SynchronizationStatus> statuses);

    /**
     * Returns every execution holding one of the supplied statuses.
     *
     * @param statuses statuses to look for
     * @return matching executions
     */
    List<Synchronization> findAllByStatusIn(Collection<SynchronizationStatus> statuses);
}
