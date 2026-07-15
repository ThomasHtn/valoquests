package io.github.thomashtn.valorant.tracker.ranking.repository;

import io.github.thomashtn.valorant.tracker.ranking.entity.WeeklyPlayerScore;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for weekly player score entities.
 */
public interface WeeklyPlayerScoreRepository extends JpaRepository<WeeklyPlayerScore, Long> {
}
