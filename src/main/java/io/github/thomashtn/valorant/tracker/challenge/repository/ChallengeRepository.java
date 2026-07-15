package io.github.thomashtn.valorant.tracker.challenge.repository;

import io.github.thomashtn.valorant.tracker.challenge.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for challenge entities.
 */
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
}
