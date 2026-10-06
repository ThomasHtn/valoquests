package io.github.thomashtn.valoquests.synchronization.repository;

import io.github.thomashtn.valoquests.synchronization.entity.SynchronizationPlayerResult;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Provides persistence operations for per-player synchronization results.
 */
public interface SynchronizationPlayerResultRepository
    extends JpaRepository<SynchronizationPlayerResult, Long> {

    /**
     * Returns player results in deterministic player order.
     *
     * <p>Fetches the lazy player too, since every caller reads its name: avoids one query per row.
     *
     * @param synchronizationId internal synchronization identifier
     * @return every player result of that execution, ordered by player identifier
     */
    @EntityGraph(attributePaths = "player")
    List<SynchronizationPlayerResult>
        findAllBySynchronizationIdOrderByPlayerIdAsc(Long synchronizationId);

    /**
     * Returns every result recorded for one player, across executions.
     *
     * @param playerId tracked player identifier
     * @return the player's results
     */
    List<SynchronizationPlayerResult> findAllByPlayerId(Long playerId);

    /**
     * Deletes the outcomes of the executions that imported nothing and finished before a cutoff.
     *
     * @param status status of a quiet execution, {@code COMPLETED}
     * @param cutoff exclusive upper bound on the end of the execution
     */
    @Modifying
    @Query(
        """
            DELETE FROM SynchronizationPlayerResult result
            WHERE result.synchronization.id IN (
                SELECT synchronization.id
                FROM Synchronization synchronization
                WHERE synchronization.status = :status
                  AND synchronization.matchesImported = 0
                  AND synchronization.finishedAt < :cutoff
            )
            """
    )
    void purgeQuietExecutionResults(
        @Param("status") SynchronizationStatus status,
        @Param("cutoff") Instant cutoff
    );
}
