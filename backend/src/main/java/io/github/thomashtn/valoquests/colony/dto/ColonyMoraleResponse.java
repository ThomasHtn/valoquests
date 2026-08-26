package io.github.thomashtn.valoquests.colony.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Exposes the morale, which is the speed the town moves at and nothing else.
 *
 * <p>Only the weekly boss moves it. Everything else the squad does is already measured by the seven-day
 * food window, and a second bar measuring the same thing would have added nothing; the fight was
 * measured nowhere.
 *
 * <p>Deliberately asymmetric: {@code growthPercentPerNight} applies on the way up alone. A demoralised
 * town falls exactly as fast as any other, because morale is a reward for winning fights, never a shield
 * against not playing.
 *
 * @param value                 today's morale
 * @param floor                 lowest morale reachable, just above zero: a run that loses every boss stalls
 * @param ceiling               highest morale reachable, where the town moves at full speed
 * @param growthPercentPerNight share of the gap tonight closes at this morale
 */
@Schema(description = "The colony's morale and the speed it buys.")
public record ColonyMoraleResponse(
    double value,
    double floor,
    double ceiling,
    double growthPercentPerNight
) {
}
