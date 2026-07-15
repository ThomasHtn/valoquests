package io.github.thomashtn.valorant.tracker.challenge.repository;

import io.github.thomashtn.valorant.tracker.challenge.entity.WeeklyChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for weekly challenge entities.
 */
public interface WeeklyChallengeRepository extends JpaRepository<WeeklyChallenge, Long> {
}
