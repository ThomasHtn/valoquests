package io.github.thomashtn.valorant.tracker.synchronization.service;

import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationResponse;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationTrigger;

/**
 * Defines administrative synchronization commands and monitoring queries.
 */
public interface SynchronizationCommandService {

    /**
     * @return completed synchronization summary
     */
    default SynchronizationResponse synchronizeAllPlayers() {
        return synchronizeAllPlayers(SynchronizationTrigger.MANUAL);
    }

    /**
     * Executes a synchronization for every active player.
     *
     * @param trigger origin of the synchronization request
     * @return completed synchronization summary
     */
    SynchronizationResponse synchronizeAllPlayers(
        SynchronizationTrigger trigger
    );

    /**
     * @param playerId player to synchronize @return synchronization summary
     */
    SynchronizationResponse synchronizePlayer(long playerId);
}
