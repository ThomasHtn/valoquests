package io.github.thomashtn.valoquests.colony.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Exposes one day of the population curve.
 *
 * <p>Carries both ceilings alongside the population, because the whole game reads off the three lines
 * together: the town hugs whichever of the two is lower, and the days they cross are the days the squad
 * had to change what it was doing.
 *
 * @param day                calendar day
 * @param runDay             that day's one-based position in the run
 * @param population         population at the end of the day
 * @param feedablePopulation inhabitants the food could feed that day
 * @param efficiency         inhabitants one point of food fed that day
 * @param materials          cumulative materials
 * @param foodStock          food of the seven days ending on this one
 * @param morale             morale the day ended on
 * @param presenceCount      players who cleared the turnout threshold that day
 */
@Schema(description = "One day of the population curve.")
public record ColonyTrajectoryPointResponse(
    LocalDate day,
    int runDay,
    int population,
    int feedablePopulation,
    double efficiency,
    int materials,
    double foodStock,
    double morale,
    int presenceCount
) {
}
