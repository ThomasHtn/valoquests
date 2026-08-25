package io.github.thomashtn.valoquests.run.repository;

import io.github.thomashtn.valoquests.run.entity.Run;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Provides persistence operations for runs.
 */
public interface RunRepository extends JpaRepository<Run, Long> {

    /**
     * Retrieves the run in progress, the only one left open at any time.
     *
     * @return the open run, or empty before the first rollover following the deployment
     */
    Optional<Run> findByClosedAtIsNull();

    /**
     * Retrieves every closed run, most recent first.
     *
     * @return closed runs ordered from the latest to the first
     */
    List<Run> findAllByClosedAtIsNotNullOrderByNumberDesc();

    /**
     * Retrieves the highest-numbered run, whether open or closed.
     *
     * @return the latest run, or empty while none was ever opened
     */
    Optional<Run> findTopByOrderByNumberDesc();

    /**
     * Retrieves the run opening on a week.
     *
     * @param firstWeekStart Monday the run's first week starts on
     * @return the run opening on that Monday, when one does
     */
    Optional<Run> findByFirstWeekStart(LocalDate firstWeekStart);

    /**
     * Opens a run unless one already holds that number or that first week.
     *
     * <p>Native, and deliberately so. Several endpoints open a run lazily and a page fires them in
     * parallel, so on a database with no run yet two requests read "none" from the same snapshot and
     * both go on to insert. {@code ON CONFLICT DO NOTHING} lets Postgres arbitrate: the loser writes
     * nothing instead of raising a constraint violation that would fail an ordinary page load.
     *
     * @param number         sequential run number
     * @param firstWeekStart Monday the run's first week starts on
     * @param lastWeekStart  Monday the run's tenth week starts on
     * @param rosterSize     roster size to freeze on the run
     */
    @Modifying
    @Query(
        value = """
            INSERT INTO run (number, first_week_start, last_week_start, roster_size)
            VALUES (:number, :firstWeekStart, :lastWeekStart, :rosterSize)
            ON CONFLICT DO NOTHING
            """,
        nativeQuery = true
    )
    void insertIfAbsent(
        @Param("number") int number,
        @Param("firstWeekStart") LocalDate firstWeekStart,
        @Param("lastWeekStart") LocalDate lastWeekStart,
        @Param("rosterSize") int rosterSize
    );
}
