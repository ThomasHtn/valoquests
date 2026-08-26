package io.github.thomashtn.valoquests.colony.model;

/**
 * How far into the day one player of the roster got.
 *
 * <p>Three states rather than two, because the threshold is the part of the turnout rule nobody guesses:
 * an evening of two deathmatches brings food in and still does not count towards the multiplier. Drawn
 * as a half-lit pip, that is read in a glance; drawn as an unlit one, it reads as "you did not play",
 * which is false and unfair.
 */
public enum ColonyPresenceState {

    /**
     * Cleared the threshold: counts towards the day's multiplier.
     */
    FULL,

    /**
     * Played, but under the threshold. The food still counted, the turnout did not.
     */
    PARTIAL,

    /**
     * Did not play.
     */
    NONE
}
