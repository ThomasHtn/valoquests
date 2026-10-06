package io.github.thomashtn.valoquests.challenge.repository;

import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Provides persistence operations for challenge selections, weekly and daily.
 */
public interface ChallengeSelectionRepository
    extends JpaRepository<ChallengeSelection, Long> {

    /**
     * Retrieves every challenge selected for one week.
     *
     * @param weekStart Monday identifying the requested week
     * @return selections ordered by identifier
     */
    @EntityGraph(attributePaths = "challenge")
    List<ChallengeSelection> findAllByWeekStartOrderByIdAsc(
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
    List<ChallengeSelection> findAllByWeekStartAndCadenceOrderByIdAsc(
        LocalDate weekStart,
        ChallengeCadence cadence
    );

    /**
     * Counts the selections of one cadence made for one week.
     *
     * @param weekStart Monday identifying the requested week
     * @param cadence   cadence of the selections to count
     * @return number of matching selections
     */
    long countByWeekStartAndCadence(LocalDate weekStart, ChallengeCadence cadence);

    /**
     * Retrieves every non-finalized challenge selected for one week.
     *
     * @param weekStart Monday identifying the requested week
     * @return active selections ordered by identifier
     */
    @EntityGraph(attributePaths = "challenge")
    List<ChallengeSelection> findAllByWeekStartAndFinalizedAtIsNullOrderByIdAsc(
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
    Optional<ChallengeSelection> findByCadenceAndDay(ChallengeCadence cadence, LocalDate day);

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
    List<ChallengeSelection> findAllByCadenceAndDayBetweenOrderByDayAsc(
        ChallengeCadence cadence,
        LocalDate firstDay,
        LocalDate lastDay
    );

    /**
     * Retrieves every weekly selection made before one week, oldest week first.
     *
     * <p>Used to replay the selection history: which challenges were already drawn in the current
     * no-repeat cycle of their tier. The week being drawn is excluded, so a pack being
     * completed one tier at a time never counts against itself.
     *
     * @param cadence   the weekly cadence
     * @param weekStart Monday identifying the week being drawn, excluded from the result
     * @return past selections ordered by week
     */
    @EntityGraph(attributePaths = "challenge")
    List<ChallengeSelection> findAllByCadenceAndWeekStartLessThanOrderByWeekStartAsc(
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
        SELECT DISTINCT selection.weekStart
        FROM ChallengeSelection selection
        WHERE selection.weekStart < :currentWeekStart
          AND selection.finalizedAt IS NULL
        ORDER BY selection.weekStart ASC
        """)
    List<LocalDate> findPendingWeekStartsBefore(
        @Param("currentWeekStart") LocalDate currentWeekStart
    );
}
