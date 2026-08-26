package io.github.thomashtn.valoquests.colony.dto;

import io.github.thomashtn.valoquests.colony.model.ColonyPresenceState;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Exposes one player of the roster and how far into today they got.
 *
 * <p>{@code rawDamage} is deliberately the figure <b>before</b> the daily diminishing returns, because
 * that is the one the threshold is read on: the returns exist to stop farming, not to decide whether
 * somebody logged in tonight.
 *
 * @param playerId  player identifier
 * @param name      player's display name
 * @param state     how far into the day they got
 * @param rawDamage raw damage they brought in today, before the daily diminishing returns
 */
@Schema(description = "One roster player's turnout for the day.")
public record ColonyPresencePlayerResponse(
    long playerId,
    String name,
    ColonyPresenceState state,
    int rawDamage
) {
}
