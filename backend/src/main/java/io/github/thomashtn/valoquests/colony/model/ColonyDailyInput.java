package io.github.thomashtn.valoquests.colony.model;

import java.time.LocalDate;

/**
 * Everything one day of a run brings the colony, already read off the persisted rows.
 *
 * <p>The whole interface between the database and the replay engine. Assembling these is the only part
 * of the colony that queries anything, which is what leaves the engine as pure arithmetic.
 *
 * <p>{@code rollover} is carried as its own flag rather than inferred from a non-zero {@code
 * creditedMaterials}: a week where nobody completed a challenge and the boss held credits exactly zero
 * materials, and it is still the Monday on which the food surplus is converted.
 *
 * @param day                 calendar day, in the week calendar's zone
 * @param matchDamage         total match damage of the day, all players confounded, already reduced by
 *     the scoring ruleset's daily diminishing returns
 * @param presencePlayerCount players whose raw damage that day cleared the turnout threshold
 * @param rollover            whether the week that just closed settles on this day
 * @param creditedMaterials   materials that rollover credits, challenges and boss together
 * @param moraleDelta         morale that rollover's fight moves, positive when it fell
 */
public record ColonyDailyInput(
    LocalDate day,
    int matchDamage,
    int presencePlayerCount,
    boolean rollover,
    int creditedMaterials,
    double moraleDelta
) {
}
