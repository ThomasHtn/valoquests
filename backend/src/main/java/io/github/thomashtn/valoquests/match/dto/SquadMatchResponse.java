package io.github.thomashtn.valoquests.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Exposes one entry of the squad's shared match history: a tracked player's match, named after the
 * player who played it.
 *
 * <p>Two tracked players in the same lobby yield two entries, one per player: each carries its own
 * statistics and its own value to the squad.
 *
 * @param playerId    internal identifier of the player who played the match
 * @param displayName name shown across the application
 * @param portrait    agent name backing the player's bundled avatar, or {@code null} when unset
 * @param match       the player's match, as the player's own history exposes it
 */
@Schema(description = "Squad match history entry: one tracked player's match.")
public record SquadMatchResponse(

    Long playerId,
    String displayName,
    String portrait,
    MatchResponse match
) {
}
