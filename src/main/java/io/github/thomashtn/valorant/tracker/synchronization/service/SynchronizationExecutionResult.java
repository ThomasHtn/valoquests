package io.github.thomashtn.valorant.tracker.synchronization.service;

import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.synchronization.model.PlayerDeepSynchronizationResult;
import io.github.thomashtn.valorant.tracker.synchronization.model.PlayerSynchronizationResult;

import java.time.Instant;

/**
 * Shared internal representation of a standard or deep synchronization result.
 *
 * @param player          synchronized player
 * @param pagesFetched    retrieved page count
 * @param matchesImported imported match count
 * @param completedAt     completion timestamp
 */
record SynchronizationExecutionResult(
    Player player,
    int pagesFetched,
    int matchesImported,
    Instant completedAt
) {

    /**
     * Creates the shared result from a standard synchronization outcome.
     *
     * @param result standard synchronization outcome
     * @return shared execution result
     */
    static SynchronizationExecutionResult from(
        PlayerSynchronizationResult result
    ) {
        return new SynchronizationExecutionResult(
            result.player(),
            result.pagesFetched(),
            result.matchesImported(),
            result.completedAt()
        );
    }

    /**
     * Creates the shared result from a deep synchronization outcome.
     *
     * @param result deep synchronization outcome
     * @return shared execution result
     */
    static SynchronizationExecutionResult from(
        PlayerDeepSynchronizationResult result
    ) {
        return new SynchronizationExecutionResult(
            result.player(),
            result.pagesFetched(),
            result.matchesImported(),
            result.completedAt()
        );
    }
}
