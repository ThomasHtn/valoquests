package io.github.thomashtn.valoquests.colony.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Exposes the colony as it stands today.
 *
 * <p>The whole model in one sentence: you play, that produces food, food says how many inhabitants the
 * town can feed, and every night the town closes part of the gap between what it has and what it could
 * feed. There is no second population rule.
 *
 * <p>One ceiling decides the score. {@code feedablePopulation} is what the food allows and the town
 * climbs towards it; nothing is wasted and there is no second ceiling to arbitrate against. {@code
 * efficiency} says how far one point of food carries, and it is the whole of what materials buy.
 *
 * <p>{@code foodStock} is a seven-day moving average, never a reserve. Today enters the count each night
 * and the day seven days back leaves it, so a quiet Tuesday dents it instead of emptying it, and three
 * intense weeks cannot be banked against a month of silence.
 *
 * @param runNumber              sequential number of the run in progress
 * @param runDay                 day of the run, from one
 * @param runDayCount            days a run spans, settlement day included
 * @param runWeekIndex           week of the run, from one
 * @param runWeekCount           weeks a run spans
 * @param day                    calendar day this state closes
 * @param population             today's population, which on the settlement day is the run's score
 * @param populationChange       inhabitants the night moved, negative when the town lost people
 * @param efficiency             inhabitants one point of food feeds, raised by the materials gathered
 * @param materials              cumulative materials, which never go back down
 * @param foodStock              food of the last seven days
 * @param feedablePopulation     inhabitants that food can feed
 * @param weeklyConsumption      food the town eats in a week
 * @param weeklySurplus          food left over once the town has eaten, never negative
 * @param presence               the day's turnout and what it multiplies the harvest by
 * @param morale                 morale and the speed it buys
 * @param tier                   step of the ladder the town sits in
 * @param nextTier               step it is climbing towards
 * @param missingEfficiency      efficiency still needed to reach that next step
 * @param tierProgressPercentage how far into the current step the town is, in {@code [0, 100]}
 * @param ladder                 the steps around the town's own, for the ladder panel
 * @param weeks                  the run's ten fights and what each was worth in efficiency
 * @param defeatedBosses         bosses put down so far in the run
 * @param bossCount              bosses a run holds
 */
@Schema(description = "The squad's colony as it stands today.")
public record ColonyResponse(
    int runNumber,
    int runDay,
    int runDayCount,
    int runWeekIndex,
    int runWeekCount,
    LocalDate day,
    int population,
    int populationChange,
    double efficiency,
    int materials,
    double foodStock,
    int feedablePopulation,
    double weeklyConsumption,
    double weeklySurplus,
    ColonyPresenceResponse presence,
    ColonyMoraleResponse morale,
    ColonyTierResponse tier,
    ColonyTierResponse nextTier,
    double missingEfficiency,
    double tierProgressPercentage,
    List<ColonyTierResponse> ladder,
    List<ColonyWeekResponse> weeks,
    int defeatedBosses,
    int bossCount
) {

    /**
     * Creates an immutable colony response.
     */
    public ColonyResponse {
        ladder = List.copyOf(ladder);
        weeks = List.copyOf(weeks);
    }
}
