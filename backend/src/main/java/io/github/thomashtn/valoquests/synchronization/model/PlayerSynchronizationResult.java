package io.github.thomashtn.valoquests.synchronization.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.player.entity.Player;
import java.time.Instant;

/**
 * Contains the result of a successful synchronization.
 *
 * @param player synchronized player
 * @param pagesFetched number of Henrik match-history pages retrieved
 * @param matchesImported number of newly imported player matches
 * @param completedAt completion timestamp
 * @param stopReason condition that ended the match-history walk
 */
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "This internal result intentionally carries the managed player aggregate to the command layer."
)
public record PlayerSynchronizationResult(

    Player player,
    int pagesFetched,
    int matchesImported,
    Instant completedAt,
    SynchronizationStopReason stopReason
) {

    /**
     * Validates the immutable result.
     */
    public PlayerSynchronizationResult {
        if (player == null) {
            throw new IllegalArgumentException("player must not be null");
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
        if (stopReason == null) {
            throw new IllegalArgumentException(
                "stopReason must not be null"
            );
        }
    }
}
