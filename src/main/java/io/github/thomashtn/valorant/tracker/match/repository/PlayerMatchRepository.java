package io.github.thomashtn.valorant.tracker.match.repository;

import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for player match entities.
 */
public interface PlayerMatchRepository extends JpaRepository<PlayerMatch, Long> {
}
