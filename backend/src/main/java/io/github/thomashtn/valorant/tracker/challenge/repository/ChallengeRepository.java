package io.github.thomashtn.valorant.tracker.challenge.repository;

import io.github.thomashtn.valorant.tracker.challenge.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Provides persistence operations for challenge catalogue entities.
 */
public interface ChallengeRepository
    extends JpaRepository<Challenge, Long> {

    /**
     * Retrieves every challenge eligible for weekly selection.
     *
     * @return enabled challenges ordered by identifier
     */
    List<Challenge> findAllByEnabledTrueOrderByIdAsc();
}
