package io.github.thomashtn.valoquests.colony.model;

import java.time.LocalDate;

/**
 * The colony at the end of one day, as the replay engine computed it.
 *
 * <p>Carries more than {@code colony_daily_snapshot} stores. The daily loss, the health and the target
 * are pure functions of the fields that are persisted, so keeping a column for them would be storing an
 * answer the table already contains; they ride here because the API reads them and the engine has just
 * computed them anyway.
 *
 * @param day               calendar day this state closes
 * @param food              Food gauge after the day, in {@code [0, 100]}
 * @param energy            Energy gauge after the day, in {@code [0, 100]}
 * @param materials         cumulative materials, which never go back down
 * @param population        population after the day's migration, in {@code [0, capacity]}
 * @param capacity          capacity the cumulative materials unlock
 * @param activePlayerCount distinct players who played at least one eligible match that day
 * @param foodGain          Food gained from the day's match damage
 * @param energyGain        Energy gained from the day's turnout
 * @param dailyLoss         amount each gauge lost before the gains applied
 * @param health            geometric mean of both gauges, in {@code [0, 1]}
 * @param target            population the colony is heading towards, {@code capacity x health}
 */
public record ColonyDayState(
    LocalDate day,
    double food,
    double energy,
    int materials,
    double population,
    int capacity,
    int activePlayerCount,
    double foodGain,
    double energyGain,
    double dailyLoss,
    double health,
    double target
) {
}
