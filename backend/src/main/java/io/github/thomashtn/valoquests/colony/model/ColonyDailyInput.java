package io.github.thomashtn.valoquests.colony.model;

import java.time.LocalDate;

/**
 * Everything one day of a run brings the colony, already read off the persisted rows.
 *
 * <p>The whole interface between the database and the replay engine. Assembling these is the only part
 * of the colony that queries anything, which is what leaves the engine as pure arithmetic.
 *
 * @param day                calendar day, in the week calendar's zone
 * @param matchDamage        total match damage of the day, all players confounded, already reduced by
 *     the scoring ruleset's daily diminishing returns
 * @param activePlayerCount  distinct players who played at least one eligible match that day
 * @param creditedMaterials  materials the weekly rollover credits on this day, zero on the six days a
 *     week that are not a rollover
 */
public record ColonyDailyInput(
    LocalDate day,
    int matchDamage,
    int activePlayerCount,
    int creditedMaterials
) {
}
