package io.github.thomashtn.valoquests.colony.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Exposes how many of the squad turned up today, and what that is worth.
 *
 * <p>The multiplier is divided by the roster frozen on the run, never by a hard seven: a five-player
 * squad reaches a full house with five, and nobody is punished because the roster shrank. What it counts
 * is players, not games — one competitive match counts exactly as much as eight — which is what makes
 * turnout a social lever rather than a second measure of time spent, and the reason the first game of
 * everybody's evening is the most valuable one of the week.
 *
 * @param present        players who cleared the threshold today
 * @param rosterSize     roster size frozen on the run
 * @param multiplier     what today's harvest is multiplied by, between one and two
 * @param nextMultiplier what the multiplier would be with one more player counted, capped at the
 *                       roster size — equal to {@code multiplier} once the roster is full, so a
 *                       client can read equality as "no next step to reach"
 * @param threshold      raw damage a player's day must clear to count
 * @param players        the roster as it currently stands, each with how far into the day they got
 */
@Schema(description = "The day's turnout and the multiplier it is worth.")
public record ColonyPresenceResponse(
    int present,
    int rosterSize,
    double multiplier,
    double nextMultiplier,
    int threshold,
    List<ColonyPresencePlayerResponse> players
) {

    /**
     * Creates an immutable presence response.
     */
    public ColonyPresenceResponse {
        players = List.copyOf(players);
    }
}
