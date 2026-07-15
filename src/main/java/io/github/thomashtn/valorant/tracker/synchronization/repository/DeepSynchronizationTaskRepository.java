package io.github.thomashtn.valorant.tracker.synchronization.repository;

import io.github.thomashtn.valorant.tracker.synchronization.entity.DeepSynchronizationTask;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for deep synchronization task entities.
 */
public interface DeepSynchronizationTaskRepository extends JpaRepository<DeepSynchronizationTask, Long> {
}
