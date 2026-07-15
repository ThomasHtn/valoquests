package io.github.thomashtn.valorant.tracker.challenge.repository;

import io.github.thomashtn.valorant.tracker.challenge.entity.PlayerChallengeProgress;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for player challenge progress entities.
 */
public interface PlayerChallengeProgressRepository extends JpaRepository<PlayerChallengeProgress, Long> {
}
