package io.github.thomashtn.valoquests.challenge.repository;

import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence operations for player challenge progress entities.
 */
public interface PlayerChallengeProgressRepository
    extends JpaRepository<PlayerChallengeProgress, Long> {

    /**
     * Finds the progress stored for one player and one weekly challenge.
     *
     * @param playerId          internal player identifier
     * @param weeklyChallengeId weekly challenge identifier
     * @return matching progress when it already exists
     */
    Optional<PlayerChallengeProgress> findByPlayerIdAndWeeklyChallengeId(
        Long playerId,
        Long weeklyChallengeId
    );

    /**
     * Retrieves the existing progress rows for one player and a group of
     * weekly challenges.
     *
     * <p>The weekly challenge association is fetched with the same query so
     * the persistence service can index results without additional lazy-load
     * queries.</p>
     *
     * @param playerId          internal player identifier
     * @param weeklyChallengeIds weekly challenge identifiers
     * @return existing progress rows
     */
    @EntityGraph(attributePaths = "weeklyChallenge")
    List<PlayerChallengeProgress> findAllByPlayerIdAndWeeklyChallengeIdIn(
        Long playerId,
        Collection<Long> weeklyChallengeIds
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
            "weeklyChallenge",
            "weeklyChallenge.challenge"
        }
    )
    List<PlayerChallengeProgress> findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(
        LocalDate weekStart
    );

}
