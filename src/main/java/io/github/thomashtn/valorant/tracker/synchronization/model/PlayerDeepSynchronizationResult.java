package io.github.thomashtn.valorant.tracker.synchronization.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valorant.tracker.player.entity.Player;

import java.time.Instant;

/**
 * Contains the result of a completed deep synchronization for one player.
 *
 * @param player          synchronized player
 * @param pagesFetched    number of Henrik pages retrieved
 * @param matchesImported number of newly imported player matches
 * @param completedAt     completion timestamp
 */
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "This internal result intentionally carries the managed player aggregate to the command layer."
)
public record PlayerDeepSynchronizationResult(

    Player player,
    int pagesFetched,
    int matchesImported,
    Instant completedAt
) {

    /**
     * Validates the deep-synchronization result.
     */
    public PlayerDeepSynchronizationResult {
        if (player == null) {
            throw new IllegalArgumentException(
                "player must not be null"
            );
        }

        if (pagesFetched < 0) {
            throw new IllegalArgumentException(
                "pagesFetched must not be negative"
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
