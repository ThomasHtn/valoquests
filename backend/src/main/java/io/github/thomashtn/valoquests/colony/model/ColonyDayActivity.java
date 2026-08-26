package io.github.thomashtn.valoquests.colony.model;

/**
 * What the squad did on one day, as the colony reads it.
 *
 * <p>Two strictly independent readings of the same evening. The damage says what was brought home and
 * is already through the scoring ruleset's daily diminishing returns; the turnout says how many of you
 * were there, and it is read on <b>raw</b> damage instead, because the diminishing returns exist to
 * stop farming and have no business deciding whether somebody logged in.
 *
 * <p>Playing more never moves the turnout. A player who fires up one competitive game counts exactly as
 * much in the multiplier as one who strings eight together, which is what makes turnout a social lever
 * rather than a second measure of time spent.
 *
 * @param matchDamage         total match damage of the day, all players confounded, already reduced by
 *     the scoring ruleset's daily diminishing returns
 * @param presencePlayerCount players whose raw damage that day cleared the turnout threshold
 */
public record ColonyDayActivity(int matchDamage, int presencePlayerCount) {

    /**
     * A day nobody played.
     */
    public static final ColonyDayActivity IDLE = new ColonyDayActivity(0, 0);
}
