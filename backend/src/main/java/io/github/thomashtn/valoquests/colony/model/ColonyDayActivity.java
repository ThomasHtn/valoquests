package io.github.thomashtn.valoquests.colony.model;

/**
 * What the squad did on one day, as the two gauges read it.
 *
 * <p>Two strictly independent behaviours: playing every day, and playing together. A gauge fed by the
 * same thing as another one would be pointless, which is why the number of matches a player played has
 * no effect whatsoever on {@code activePlayerCount}.
 *
 * @param matchDamage       total match damage of the day, all players confounded, already reduced by
 *     the scoring ruleset's daily diminishing returns
 * @param activePlayerCount distinct players who played at least one eligible match that day
 */
public record ColonyDayActivity(int matchDamage, int activePlayerCount) {

    /**
     * A day nobody played.
     */
    public static final ColonyDayActivity IDLE = new ColonyDayActivity(0, 0);
}
