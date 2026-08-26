package io.github.thomashtn.valoquests.colony.dto;

import io.github.thomashtn.valoquests.colony.model.ColonyGauge;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Exposes the colony as it stands today.
 *
 * <p>{@code limitingGauge} and {@code equilibriumPercentage} carry the one property of the model that
 * cannot be read off the gauges themselves: the population settles at
 * {@code capacity x min(Food gain, Energy gain) / 14}, so the gauge that is fed less alone decides how
 * full the colony can get, and the other one saturates and loses its surplus.
 *
 * <p>Everything settling-related is resolved against the <b>last seven complete days</b>, never against
 * today. Today is a day in progress: before anybody has played it holds no damage and no turnout, so an
 * equilibrium read off it would collapse to zero every morning and climb back through the evening,
 * which is exactly the reading a permanent display must not give.
 *
 * @param runNumber             sequential number of the run in progress
 * @param runDay                day of the run, from one
 * @param runDayCount           days a run spans, settlement day included
 * @param runWeekIndex          week of the run, from one
 * @param runWeekCount          weeks a run spans
 * @param day                   calendar day this state closes
 * @param food                  Food gauge and today's movement on it
 * @param energy                Energy gauge and today's movement on it
 * @param upkeep                what the colony is about to consume, and what covers it
 * @param healthPercentage      geometric mean of both gauges, in {@code [0, 100]}
 * @param alert                 whether health has fallen under the distress threshold; a display flag
 *     with no mechanical effect
 * @param population            today's population
 * @param targetPopulation      population the colony is heading towards
 * @param populationChange      inhabitants gained or lost today
 * @param dailyMigrationLimit   most inhabitants a single day can bring in
 * @param capacity              capacity the erected buildings open
 * @param maximumCapacity       capacity of the last tier, a run's theoretical maximum score
 * @param materials             cumulative materials
 * @param buildings             every tier, erected or not
 * @param nextTier              tier being worked towards, {@code null} once the last one is up
 * @param limitingGauge         gauge currently setting the equilibrium population
 * @param equilibriumPercentage share of capacity the colony plateaus at if the last seven days' rhythm
 *     holds
 * @param defeatedBosses        bosses put down so far in the run
 * @param bossCount             bosses a run holds
 * @param materialsPerBoss      materials one defeated boss brings in
 */
@Schema(description = "The squad's colony as it stands today.")
public record ColonyResponse(
    int runNumber,
    int runDay,
    int runDayCount,
    int runWeekIndex,
    int runWeekCount,
    LocalDate day,
    ColonyGaugeResponse food,
    ColonyGaugeResponse energy,
    ColonyUpkeepResponse upkeep,
    double healthPercentage,
    boolean alert,
    int population,
    int targetPopulation,
    int populationChange,
    int dailyMigrationLimit,
    int capacity,
    int maximumCapacity,
    int materials,
    List<ColonyBuildingResponse> buildings,
    ColonyNextTierResponse nextTier,
    ColonyGauge limitingGauge,
    double equilibriumPercentage,
    int defeatedBosses,
    int bossCount,
    int materialsPerBoss
) {
    /**
     * Creates an immutable colony response.
     */
    public ColonyResponse {
        buildings = List.copyOf(buildings);
    }
}
