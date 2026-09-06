package io.github.thomashtn.valoquests.synchronization.service;

/**
 * Outcome of synchronizing one player, carrying the batch aggregate updated with that player's
 * result and the original failure, when the player's synchronization failed.
 *
 * @param summary batch summary updated with this player's outcome
 * @param failure original exception, {@code null} when the player synchronized successfully
 */
record PlayerSynchronizationOutcome(
    SynchronizationBatchSummary summary,
    RuntimeException failure
) {
}
