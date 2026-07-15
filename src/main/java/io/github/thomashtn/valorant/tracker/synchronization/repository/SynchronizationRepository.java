package io.github.thomashtn.valorant.tracker.synchronization.repository;

import io.github.thomashtn.valorant.tracker.synchronization.entity.Synchronization;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for synchronization entities.
 */
public interface SynchronizationRepository extends JpaRepository<Synchronization, Long> {
}
