package io.github.thomashtn.valoquests.colony.model;

/**
 * How one week of a run's ten fights ended.
 */
public enum ColonyWeekOutcomeState {

    /**
     * The boss was put down: it paid its materials and lifted the morale.
     */
    DEFEATED,

    /**
     * The boss held. It paid nothing, and it cost morale.
     */
    SURVIVED,

    /**
     * The fight is under way.
     */
    CURRENT,

    /**
     * The week has not been reached yet.
     */
    UPCOMING
}
