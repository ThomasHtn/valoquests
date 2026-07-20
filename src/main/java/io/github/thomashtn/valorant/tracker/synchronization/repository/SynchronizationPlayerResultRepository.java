package io.github.thomashtn.valorant.tracker.synchronization.repository;

import io.github.thomashtn.valorant.tracker.synchronization.entity.SynchronizationPlayerResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for per-player synchronization results.
 */
public interface SynchronizationPlayerResultRepository
    extends JpaRepository<SynchronizationPlayerResult, Long> {

    /**
     * Returns player results in deterministic player order.
     */
    List<SynchronizationPlayerResult>
        findAllBySynchronizationIdOrderByPlayerIdAsc(Long synchronizationId);
}
