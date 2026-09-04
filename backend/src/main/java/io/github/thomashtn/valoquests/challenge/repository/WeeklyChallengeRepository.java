package io.github.thomashtn.valoquests.challenge.repository;

import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
     * Retrieves the selections of one cadence made for one week.
     *
     * @param weekStart Monday identifying the requested week
     * @param cadence   cadence of the selections wanted
     * @return matching selections ordered by identifier
     */
    @EntityGraph(attributePaths = "challenge")
    List<WeeklyChallenge> findAllByWeekStartAndCadenceOrderByIdAsc(
        LocalDate weekStart,
        ChallengeCadence cadence
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
     * Retrieves the daily selection covering one day.
     *
     * @param cadence the daily cadence
     * @param day     day the selection covers
     * @return the day's selection when it was drawn
     */
    @EntityGraph(attributePaths = "challenge")
    Optional<WeeklyChallenge> findByCadenceAndDay(ChallengeCadence cadence, LocalDate day);

    /**
     * Retrieves the daily selections covering a range of days, oldest first.
     *
     * <p>Used by the daily draw's no-repeat window, and by the interface's week strip.
     *
     * @param cadence  the daily cadence
     * @param firstDay first day of the range, inclusive
     * @param lastDay  last day of the range, inclusive
     * @return selections ordered by day
     */
    @EntityGraph(attributePaths = "challenge")
    List<WeeklyChallenge> findAllByCadenceAndDayBetweenOrderByDayAsc(
        ChallengeCadence cadence,
        LocalDate firstDay,
        LocalDate lastDay
    );

    /**
     * Retrieves every weekly selection made before one week, oldest week first.
     *
     * <p>Used to replay the selection history: which challenges were already drawn in the current
     * no-repeat cycle of their difficulty. The week being drawn is excluded, so a pack being
     * completed one difficulty at a time never counts against itself.
     *
     * @param cadence   the weekly cadence
     * @param weekStart Monday identifying the week being drawn, excluded from the result
     * @return past selections ordered by week
     */
    @EntityGraph(attributePaths = "challenge")
    List<WeeklyChallenge> findAllByCadenceAndWeekStartLessThanOrderByWeekStartAsc(
        ChallengeCadence cadence,
        LocalDate weekStart
    );

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

    /**
     * Returns every selection whose week falls inside a range, both cadences, oldest week first.
     *
     * <p>What the campaign replay reads: it settles ten weeks in one pass, and asking per week made
     * a call that runs after every synchronization cost ten round trips instead of one.
     *
     * @param firstWeekStart first Monday of the range, inclusive
     * @param lastWeekStart  last Monday of the range, inclusive
     * @return the selections in week order
     */
    List<WeeklyChallenge> findAllByWeekStartBetweenOrderByWeekStartAsc(
        LocalDate firstWeekStart,
        LocalDate lastWeekStart
    );
}
