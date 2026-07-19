package io.github.thomashtn.valorant.tracker.synchronization.model;

import io.github.thomashtn.valorant.tracker.player.entity.Player;
import java.time.Instant;

/**
 * Contains the result of a successful synchronization for one player.
 *
 * @param player synchronized player
 * @param matchesImported number of newly imported player matches
 * @param completedAt completion timestamp
 */
public record PlayerSynchronizationResult(
    Player player,
    int matchesImported,
    Instant completedAt
) {

    /**
     * Creates a synchronization result.
     *
     * @throws IllegalArgumentException when the imported match count is
     *                                  negative
     */
    public PlayerSynchronizationResult {
        if (player == null) {
            throw new IllegalArgumentException(
                "player must not be null"
            );
        }

        if (matchesImported < 0) {
            throw new IllegalArgumentException(
                "matchesImported must not be negative"
            );
        }

        if (completedAt == null) {
            throw new IllegalArgumentException(
                "completedAt must not be null"
            );
        }
    }
}
