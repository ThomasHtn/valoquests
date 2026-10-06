package io.github.thomashtn.valoquests.challenge.repository;

import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for player challenge progress entities.
 */
public interface PlayerChallengeProgressRepository
    extends JpaRepository<PlayerChallengeProgress, Long> {

    /**
     * Retrieves the existing progress rows for one player and a group of
     * challenge selections.
     *
     * <p>The selection association is fetched with the same query so
     * {@code PlayerChallengeProgressWriter} can index results without additional lazy-load
     * queries.</p>
     *
     * @param playerId     internal player identifier
     * @param selectionIds challenge selection identifiers
     * @return existing progress rows
     */
    @EntityGraph(attributePaths = "selection")
    List<PlayerChallengeProgress> findAllByPlayerIdAndSelectionIdIn(
        Long playerId,
        Collection<Long> selectionIds
    );

    /**
     * Retrieves every persisted progress row for one calendar week.
     *
     * <p>The player, weekly challenge and catalogue challenge associations are
     * fetched eagerly to support ranking aggregation without N+1 queries.</p>
     *
     * @param weekStart Monday identifying the requested week
     * @return progress rows for the week
     */
    @EntityGraph(
        attributePaths = {
            "player",
            "selection",
            "selection.challenge"
        }
    )
    List<PlayerChallengeProgress> findAllBySelectionWeekStartOrderByPlayerIdAscSelectionIdAsc(
        LocalDate weekStart
    );


    /**
     * Returns every completed progress row whose week falls inside a range.
     *
     * <p>The campaign replay's only reading of the challenges: what each player validated, over
     * the whole campaign, in one query. Incomplete rows are left out because they pay nothing.
     *
     * @param firstWeekStart first Monday of the range, inclusive
     * @param lastWeekStart  last Monday of the range, inclusive
     * @return the completed rows, oldest identifier first
     */
    List<PlayerChallengeProgress> findAllByCompletedTrueAndSelectionWeekStartBetweenOrderByIdAsc(
        LocalDate firstWeekStart,
        LocalDate lastWeekStart
    );

    /**
     * Returns every challenge progress of one player.
     *
     * @param playerId internal player identifier
     * @return the player's progress rows
     */
    List<PlayerChallengeProgress> findAllByPlayerId(Long playerId);
}
