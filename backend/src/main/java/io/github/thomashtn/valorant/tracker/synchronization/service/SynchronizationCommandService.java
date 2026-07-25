package io.github.thomashtn.valorant.tracker.synchronization.service;

import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationResponse;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationTrigger;

/**
 * Defines administrative synchronization commands and monitoring queries.
 */
public interface SynchronizationCommandService {

    /**
     * @return completed standard synchronization summary
     */
    default SynchronizationResponse synchronizeAllPlayers() {
        return synchronizeAllPlayers(SynchronizationTrigger.MANUAL);
    }

    /**
     * Executes a standard synchronization for every active player.
     *
     * @param trigger origin of the synchronization request
     * @return completed standard synchronization summary
     */
    SynchronizationResponse synchronizeAllPlayers(
        SynchronizationTrigger trigger
    );

    /**
     * @param playerId player to synchronize @return synchronization summary
     */
    SynchronizationResponse synchronizePlayer(long playerId);

    /**
     * @return accepted deep-synchronization request
     */
    SynchronizationResponse requestDeepSynchronizationForAllPlayers();

    /**
     * @param playerId player to import deeply @return accepted synchronization request
     */
    SynchronizationResponse requestDeepSynchronizationForPlayer(long playerId);
}
