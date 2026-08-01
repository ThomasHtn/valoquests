package io.github.thomashtn.valorant.tracker.synchronization.repository;

import io.github.thomashtn.valorant.tracker.synchronization.entity.SynchronizationPlayerResult;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for per-player synchronization results.
 */
public interface SynchronizationPlayerResultRepository
    extends JpaRepository<SynchronizationPlayerResult, Long> {

    /**
     * Returns player results in deterministic player order.
     *
     * <p>The player is fetched alongside its results because every caller reads its display name.
     * The association is lazy, which would otherwise issue one extra query per returned row.
     *
     * @param synchronizationId internal synchronization identifier
     * @return every player result of that execution, ordered by player identifier
     */
    @EntityGraph(attributePaths = "player")
    List<SynchronizationPlayerResult>
        findAllBySynchronizationIdOrderByPlayerIdAsc(Long synchronizationId);
}
