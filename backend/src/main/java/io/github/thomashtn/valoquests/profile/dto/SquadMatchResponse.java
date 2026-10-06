package io.github.thomashtn.valoquests.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Exposes one squad match history entry: a tracked player's match, named after that player.
 *
 * <p>Two tracked players in the same lobby yield two entries, each with its own statistics and value.
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
