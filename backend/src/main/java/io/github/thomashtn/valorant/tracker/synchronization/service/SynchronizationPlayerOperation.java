package io.github.thomashtn.valorant.tracker.synchronization.service;

/**
 * Executes one player-level synchronization operation.
 */
@FunctionalInterface
interface SynchronizationPlayerOperation {

    /**
     * Synchronizes one player.
     *
     * @param playerId player identifier
     * @return successful synchronization result
     */
    SynchronizationExecutionResult synchronize(long playerId);
}
