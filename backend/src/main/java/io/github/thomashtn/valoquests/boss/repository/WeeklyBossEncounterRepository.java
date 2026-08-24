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
     * Retrieves every encounter ever created, oldest week first.
     *
     * <p>Used to replay the boss-selection history: which bosses were already drawn in the current
     * no-repeat cycle.
     *
     * @return every encounter ordered by week
     */
    List<WeeklyBossEncounter> findAllByOrderByWeekStartAsc();

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
     * Retrieves finalized encounters using week-based pagination, most recent week first.
     *
     * @param pageable page request
     * @return page of finalized encounters
     */
    @EntityGraph(attributePaths = {"bossCatalogEntry", "defeatedByPlayer"})
    Page<WeeklyBossEncounter> findAllByFinalizedAtIsNotNullOrderByWeekStartDesc(Pageable pageable);

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
