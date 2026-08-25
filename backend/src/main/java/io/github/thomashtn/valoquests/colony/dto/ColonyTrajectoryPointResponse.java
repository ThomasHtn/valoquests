package io.github.thomashtn.valoquests.colony.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Exposes one day of the population curve.
 *
 * @param day               calendar day
 * @param runDay            day of the run, from one
 * @param population        population at the end of the day
 * @param capacity          capacity at the end of the day
 * @param materials         cumulative materials at the end of the day
 * @param food              Food gauge at the end of the day
 * @param energy            Energy gauge at the end of the day
 * @param activePlayerCount distinct players who played at least one eligible match that day
 */
@Schema(description = "One day of a run's population curve.")
public record ColonyTrajectoryPointResponse(
    LocalDate day,
    int runDay,
    int population,
    int capacity,
    int materials,
    double food,
    double energy,
    int activePlayerCount
) {
}
