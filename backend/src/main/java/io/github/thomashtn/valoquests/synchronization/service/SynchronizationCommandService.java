package io.github.thomashtn.valoquests.synchronization.service;

import io.github.thomashtn.valoquests.synchronization.model.SynchronizationTrigger;

/**
 * Executes synchronizations on the calling thread, for the schedulers and the background runner.
 */
public interface SynchronizationCommandService {

    /**
     * Executes a synchronization for every tracked player who is not archived.
     *
     * @param trigger origin of the synchronization request
     */
    void synchronizeAllPlayers(
        SynchronizationTrigger trigger
    );

    /**
     * Synchronizes one tracked player.
     *
     * <p>A player failure is recorded on the execution row, never thrown.
     *
     * @param playerId player to synchronize
     */
    void synchronizePlayer(long playerId);
}
