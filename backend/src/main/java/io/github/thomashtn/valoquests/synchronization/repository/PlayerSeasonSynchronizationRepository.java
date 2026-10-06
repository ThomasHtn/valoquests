package io.github.thomashtn.valoquests.synchronization.repository;

import io.github.thomashtn.valoquests.synchronization.entity.PlayerSeasonSynchronization;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides access to per-player season synchronization state.
 */
public interface PlayerSeasonSynchronizationRepository
    extends JpaRepository<PlayerSeasonSynchronization, Long> {

    /**
     * Finds the state of one season for one player.
     *
     * @param playerId tracked player identifier
     * @param seasonId local season identifier
     * @return the stored state, or empty when the season was never walked
     */
    Optional<PlayerSeasonSynchronization> findByPlayerIdAndSeasonId(
        Long playerId,
        Long seasonId
    );

    /**
     * Finds the state of one season addressed by its Henrik identifier.
     *
     * <p>Keyed on the external identifier so the lookup creates no season row at a season boundary.
     *
     * @param playerId tracked player identifier
     * @param seasonExternalId Henrik season identifier
     * @return the stored state, or empty when the season was never walked
     */
    Optional<PlayerSeasonSynchronization> findByPlayerIdAndSeasonExternalId(
        Long playerId,
        String seasonExternalId
    );

    /**
     * Returns every season walk checkpoint of one player.
     *
     * @param playerId tracked player identifier
     * @return the player's checkpoints
     */
    List<PlayerSeasonSynchronization> findAllByPlayerId(Long playerId);
}
