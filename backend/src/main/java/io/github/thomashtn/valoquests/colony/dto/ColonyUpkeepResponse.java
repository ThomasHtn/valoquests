package io.github.thomashtn.valoquests.colony.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Exposes what the colony is about to eat, and what it takes to cover it.
 *
 * <p>Forward-looking on purpose. The loss of a day is charged in full when the day opens, so a figure
 * describing what has already been taken tells a player nothing they can act on; what they can act on
 * is the next one. Stating it alongside the damage and the turnout that cancel it turns the gauges from
 * a readout into the evening's objective.
 *
 * <p>Both requirements are needed, never either one. Damage fills Food, turnout fills Energy, and a day
 * that brings only one of them still lets the other gauge fall.
 *
 * @param upcomingLoss  what the next daily tick takes off each gauge, {@code 14 x (population /
 *     capacity)}
 * @param damageToHold  match damage a day needs for Food to break even
 * @param matchesToHold that damage expressed in ordinary competitive games
 * @param playersToHold distinct players a day needs for Energy to break even, never above the roster
 */
@Schema(description = "What the colony is about to consume, and what it takes to cover it.")
public record ColonyUpkeepResponse(
    double upcomingLoss,
    int damageToHold,
    int matchesToHold,
    int playersToHold
) {
}
