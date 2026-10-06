package io.github.thomashtn.valoquests.synchronization.service;

import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.synchronization.model.PlayerSynchronizationResult;

/**
 * Immutable aggregate of all player outcomes in one synchronization batch.
 *
 * @param successfulPlayers successful player count
 * @param failureCount      failed player count
 * @param matchesImported   total imported match count
 * @param errorMessages     aggregated failure descriptions
 */
record SynchronizationBatchSummary(
    int successfulPlayers,
    int failureCount,
    int matchesImported,
    String errorMessages
) {

    /**
     * Creates an empty summary.
     *
     * @return empty summary
     */
    static SynchronizationBatchSummary empty() {
        return new SynchronizationBatchSummary(0, 0, 0, null);
    }

    /**
     * Adds a successful player outcome.
     *
     * @param result successful outcome
     * @return updated summary
     */
    SynchronizationBatchSummary withSuccess(
        PlayerSynchronizationResult result
    ) {
        return new SynchronizationBatchSummary(
            successfulPlayers + 1,
            failureCount,
            matchesImported + result.matchesImported(),
            errorMessages
        );
    }

    /**
     * Adds a failed player outcome.
     *
     * @param player       failed player
     * @param errorMessage failure description
     * @return updated summary
     */
    SynchronizationBatchSummary withFailure(
        Player player,
        String errorMessage
    ) {
        String playerError = "Player "
            + player.getId()
            + ": "
            + errorMessage;
        String updatedErrors = errorMessages == null
            ? playerError
            : errorMessages + System.lineSeparator() + playerError;

        return new SynchronizationBatchSummary(
            successfulPlayers,
            failureCount + 1,
            matchesImported,
            updatedErrors
        );
    }
}
