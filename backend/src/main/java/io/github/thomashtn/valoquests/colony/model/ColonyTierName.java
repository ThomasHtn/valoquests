package io.github.thomashtn.valoquests.colony.model;

/**
 * The names a town wears as it grows, from the smallest to the largest.
 *
 * <p>Purely decorative: crossing a tier changes no rule whatsoever, it only gives the run a milestone
 * roughly once a week. The names are absolute values rather than shares of anything, so a twenty-player
 * squad genuinely opens its run several tiers up — its town really is bigger on day one.
 *
 * <p>{@link #CITADEL} is the open end of the ladder: past it every further step is a numbered citadel,
 * which is what lets the tiers run on without a maximum. Every other constant covers exactly one step.
 */
public enum ColonyTierName {

    /**
     * Everything up to the first named step, so a small roster's opening town still has a name.
     */
    CAMP,

    /**
     * Second step.
     */
    HAMLET,

    /**
     * Third step.
     */
    VILLAGE,

    /**
     * Fourth step.
     */
    BOROUGH,

    /**
     * Fifth step.
     */
    TOWN,

    /**
     * Sixth step.
     */
    CITY,

    /**
     * Seventh step.
     */
    RESIDENTIAL_QUARTER,

    /**
     * Eighth step.
     */
    GREAT_CITY,

    /**
     * Ninth step.
     */
    METROPOLIS,

    /**
     * Tenth step.
     */
    MEGALOPOLIS,

    /**
     * Eleventh step.
     */
    CAPITAL,

    /**
     * Twelfth step and every one after it, numbered by {@link ColonyTier#level()}.
     */
    CITADEL
}
