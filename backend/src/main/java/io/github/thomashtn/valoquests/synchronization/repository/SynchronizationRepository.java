package io.github.thomashtn.valoquests.synchronization.repository;

import io.github.thomashtn.valoquests.synchronization.entity.Synchronization;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * Returns the execution holding one of the supplied statuses that finished last.
     *
     * @param statuses statuses to look for
     * @return the latest finished execution, or empty when none matches
     */
    Optional<Synchronization> findFirstByStatusInAndFinishedAtNotNullOrderByFinishedAtDescIdDesc(
        Collection<SynchronizationStatus> statuses
    );

    /**
     * Returns when the last execution holding one of the statuses and importing matches finished.
     *
     * @param statuses statuses to look for
     * @return the end of that execution, or empty when none imported anything
     */
    @Query(
        """
            SELECT MAX(synchronization.finishedAt)
            FROM Synchronization synchronization
            WHERE synchronization.status IN :statuses
              AND synchronization.matchesImported > 0
            """
    )
    Optional<Instant> findLastImportFinishedAt(
        @Param("statuses") Collection<SynchronizationStatus> statuses
    );

    /**
     * Determines whether an execution currently holds one of the supplied statuses.
     *
     * <p>Backs the public "synchronization in progress" flag; exclusivity itself comes from
     * {@code MatchHistoryLock}.
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

    /**
     * Deletes the executions that imported nothing and finished before a cutoff.
     *
     * <p>Their per-player outcomes must be deleted first.
     *
     * @param status status of a quiet execution, {@code COMPLETED}
     * @param cutoff exclusive upper bound on the end instant
     * @return number of executions deleted
     */
    @Modifying
    @Query(
        """
            DELETE FROM Synchronization synchronization
            WHERE synchronization.status = :status
              AND synchronization.matchesImported = 0
              AND synchronization.finishedAt < :cutoff
            """
    )
    int purgeQuietExecutions(
        @Param("status") SynchronizationStatus status,
        @Param("cutoff") Instant cutoff
    );
}
