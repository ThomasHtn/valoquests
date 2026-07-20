package io.github.thomashtn.valorant.tracker.challenge.repository;

import io.github.thomashtn.valorant.tracker.challenge.entity.WeeklyChallenge;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Provides persistence operations for weekly challenge entities.
 */
public interface WeeklyChallengeRepository
    extends JpaRepository<WeeklyChallenge, Long> {

    /**
     * Retrieves every challenge selected for one week.
     *
     * @param weekStart Monday identifying the requested week
     * @return weekly challenges ordered by identifier
     */
    @EntityGraph(attributePaths = "challenge")
    List<WeeklyChallenge> findAllByWeekStartOrderByIdAsc(
        LocalDate weekStart
    );

    /**
     * Retrieves every non-finalized challenge selected for one week.
     *
     * @param weekStart Monday identifying the requested week
     * @return active weekly challenges ordered by identifier
     */
    @EntityGraph(attributePaths = "challenge")
    List<WeeklyChallenge>
    findAllByWeekStartAndFinalizedAtIsNullOrderByIdAsc(
        LocalDate weekStart
    );
}
