package io.github.thomashtn.valorant.tracker.synchronization.repository;

import io.github.thomashtn.valorant.tracker.synchronization.entity.PlayerSeasonSynchronization;
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
     * <p>Used at a season boundary to decide whether an older season must still be walked.
     * Deliberately keyed on the external identifier so the lookup creates nothing: a season never
     * targeted before has no local row, returns empty, and is therefore left alone.
     *
     * @param playerId tracked player identifier
     * @param seasonExternalId Henrik season identifier
     * @return the stored state, or empty when the season was never walked
     */
    Optional<PlayerSeasonSynchronization> findByPlayerIdAndSeasonExternalId(
        Long playerId,
        String seasonExternalId
    );
}
