package io.github.thomashtn.valorant.tracker.challenge.repository;

import io.github.thomashtn.valorant.tracker.challenge.entity.WeeklyChallenge;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    List<WeeklyChallenge> findAllByWeekStartAndFinalizedAtIsNullOrderByIdAsc(
        LocalDate weekStart
    );

    /**
     * Checks whether at least one challenge exists for a week.
     *
     * @param weekStart Monday identifying the requested week
     * @return {@code true} when the weekly pack already exists
     */
    boolean existsByWeekStart(LocalDate weekStart);

    /**
     * Retrieves every past week still holding an active challenge pack.
     *
     * <p>A week appears here until its whole pack is finalized, so a rollover that never ran keeps
     * its week pending instead of losing it: the next rollover finds it and catches it up.</p>
     *
     * @param currentWeekStart Monday identifying the week in progress, excluded from the result
     * @return pending week identifiers, oldest first
     */
    @Query("""
        SELECT DISTINCT weeklyChallenge.weekStart
        FROM WeeklyChallenge weeklyChallenge
        WHERE weeklyChallenge.weekStart < :currentWeekStart
          AND weeklyChallenge.finalizedAt IS NULL
        ORDER BY weeklyChallenge.weekStart ASC
        """)
    List<LocalDate> findPendingWeekStartsBefore(
        @Param("currentWeekStart") LocalDate currentWeekStart
    );
}
