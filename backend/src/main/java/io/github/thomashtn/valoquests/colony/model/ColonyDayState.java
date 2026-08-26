package io.github.thomashtn.valoquests.colony.model;

import java.time.LocalDate;

/**
 * The colony as one day of a run leaves it.
 *
 * <p>Everything the model needs to carry to the next day, plus the two figures a page reads it by. What
 * is left out is left out because it is a multiplication away: what the town can feed is {@code
 * foodStock x efficiency}, what it eats in a week is {@code population / efficiency}, and the tier it
 * sits in follows from the efficiency alone. Persisting either would let a stored figure disagree with
 * the rule producing it.
 *
 * @param day                 calendar day this state closes
 * @param foodStock           food of the last seven days, the whole of what the town has to eat
 * @param foodHarvest         what this day alone brought in, turnout multiplier included
 * @param matchDamage         match damage of the day, kept so the calibration can be revisited later
 * @param presencePlayerCount players who cleared the turnout threshold that day
 * @param morale              morale the day ends on, between the ruleset's floor and its ceiling
 * @param materials           cumulative materials, which never decrease
 * @param efficiency          inhabitants one point of food feeds, raised by the materials gathered
 * @param population          inhabitants the night leaves behind
 * @param populationChange    what the night moved, negative when the town lost people
 */
public record ColonyDayState(
    LocalDate day,
    double foodStock,
    double foodHarvest,
    int matchDamage,
    int presencePlayerCount,
    double morale,
    int materials,
    double efficiency,
    double population,
    double populationChange
) {
}
