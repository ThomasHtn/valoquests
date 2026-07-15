package io.github.thomashtn.valorant.tracker.synchronization.repository;

import io.github.thomashtn.valorant.tracker.synchronization.entity.SynchronizationPlayerResult;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for synchronization player result entities.
 */
public interface SynchronizationPlayerResultRepository extends JpaRepository<SynchronizationPlayerResult, Long> {
}
