package io.github.thomashtn.valorant.tracker.challenge.repository;

import io.github.thomashtn.valorant.tracker.challenge.entity.PlayerChallengeProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

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
    Optional<PlayerChallengeProgress>
    findByPlayerIdAndWeeklyChallengeId(
        Long playerId,
        Long weeklyChallengeId
    );
}
