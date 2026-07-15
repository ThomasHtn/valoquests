package io.github.thomashtn.valorant.tracker.match.repository;

import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for valorant match entities.
 */
public interface ValorantMatchRepository extends JpaRepository<ValorantMatch, Long> {
}
