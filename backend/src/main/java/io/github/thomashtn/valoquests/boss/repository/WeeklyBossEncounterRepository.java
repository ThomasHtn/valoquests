package io.github.thomashtn.valoquests.boss.repository;

import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Provides persistence operations for weekly boss encounters.
 */
public interface WeeklyBossEncounterRepository
    extends JpaRepository<WeeklyBossEncounter, Long> {

    /**
     * Retrieves the encounter selected for one week, when it exists.
     *
     * @param weekStart Monday identifying the requested week
     * @return matching encounter, when the boss for that week was already drawn
     */
    @EntityGraph(attributePaths = "bossCatalogEntry")
    Optional<WeeklyBossEncounter> findByWeekStart(LocalDate weekStart);

    /**
     * Retrieves one run's encounters, oldest week first.
     *
     * <p>The no-repeat cycle is replayed over the campaign in progress rather than over the whole
     * history: a new run restarts the campaign, and a campaign opening on only the bosses its
     * predecessor had not reached yet would face a shrinking catalogue instead of a fresh run.
     *
     * @param runId run the campaign currently runs in
     * @return that run's encounters ordered by week
     */
    List<WeeklyBossEncounter> findAllByRunIdOrderByWeekStartAsc(Long runId);

    /**
     * Retrieves one run's encounters that were finalized, oldest week first.
     *
     * <p>Feeds the colony's materials: a run's ten weeks are walked in order to credit four hundred
     * materials for each boss the group put down.
     *
     * @param runId run to read
     * @return that run's finalized encounters ordered by week
     */
    List<WeeklyBossEncounter> findAllByRunIdAndFinalizedAtIsNotNullOrderByWeekStartAsc(Long runId);

    /**
     * Retrieves one run's fights that were never settled, from a week on.
     *
     * <p>What an operator stopping a campaign leaves behind. A fight is settled by the rollover that
     * follows it, so a run stopped mid-week never settles the week it stopped in: the encounter stays
     * open, pays nobody, and — encounters being unique per week — would keep the run opened in its
     * place from ever drawing a boss of its own for that week.
     *
     * @param runId     run being stopped
     * @param weekStart Monday from which the fights are released, included
     * @return that run's unsettled encounters from that week on
     */
    List<WeeklyBossEncounter> findAllByRunIdAndFinalizedAtIsNullAndWeekStartGreaterThanEqual(
        Long runId,
        LocalDate weekStart
    );

    /**
     * Deletes one run's encounters, so the run itself can be deleted after them.
     *
     * @param runId run being deleted
     */
    void deleteAllByRunId(Long runId);

    /**
     * Retrieves every past encounter whose fight was never resolved, oldest week first.
     *
     * <p>Drives the rollover's boss closure. Reading the encounters themselves rather than the weeks
     * whose challenge pack is still open is what makes the closure catch up on its own: an encounter
     * belonging to a week that was finalized without it — a rollover interrupted after the pack was
     * frozen, a week whose boss was drawn by a page view after its pack had closed — is otherwise
     * never looked at again, and its fight stays unresolved forever.
     *
     * @param currentWeekStart Monday identifying the week in progress, excluded
     * @return unresolved encounters of past weeks, ordered by week
     */
    @EntityGraph(attributePaths = "bossCatalogEntry")
    List<WeeklyBossEncounter> findAllByFinalizedAtIsNullAndWeekStartLessThanOrderByWeekStartAsc(
        LocalDate currentWeekStart
    );

    /**
     * Retrieves one run's finalized encounters using week-based pagination, most recent week first.
     *
     * @param runId    run the campaign currently runs in
     * @param pageable page request
     * @return page of that run's finalized encounters
     */
    @EntityGraph(attributePaths = {"bossCatalogEntry", "defeatedByPlayer"})
    Page<WeeklyBossEncounter> findAllByRunIdAndFinalizedAtIsNotNullOrderByWeekStartDesc(
        Long runId,
        Pageable pageable
    );

    /**
     * Retrieves the most recently finalized encounters that recorded how many players faced them.
     *
     * <p>Feeds the calibration of a new fight from what the roster actually produces. Encounters with
     * no recorded roster size predate that column and would divide by zero, so they are excluded here
     * rather than guarded for at every call site.
     *
     * @param pageable page request bounding how far back calibration looks
     * @return finalized encounters, most recent week first
     */
    @Query(
        "SELECT encounter FROM WeeklyBossEncounter encounter "
            + "WHERE encounter.finalizedAt IS NOT NULL "
            + "AND encounter.activePlayerCount > 0 "
            + "ORDER BY encounter.weekStart DESC"
    )
    List<WeeklyBossEncounter> findRecentFinalized(Pageable pageable);

    /**
     * Returns the earliest week a boss was ever drawn for.
     *
     * <p>Marks the instant from which a stored match can have dealt boss damage. Everything before
     * it predates the campaign entirely.
     *
     * @return the oldest encounter's week, or empty when no boss was ever drawn
     */
    @Query("SELECT MIN(encounter.weekStart) FROM WeeklyBossEncounter encounter")
    Optional<LocalDate> findEarliestWeekStart();

    /**
     * Determines whether a player is credited with ending a boss encounter.
     *
     * @param playerId tracked player identifier
     * @return {@code true} when at least one encounter names the player as its finisher
     */
    boolean existsByDefeatedByPlayerId(Long playerId);
}
