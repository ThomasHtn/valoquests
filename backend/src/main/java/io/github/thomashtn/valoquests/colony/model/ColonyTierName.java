package io.github.thomashtn.valoquests.colony.model;

/**
 * The names a town wears as it grows, from the smallest to the largest.
 *
 * <p>Purely decorative: crossing a tier changes no rule whatsoever, it only gives the run a milestone
 * roughly once a week. The names are absolute values rather than shares of anything, so a twenty-player
 * squad genuinely opens its run several tiers up — its town really is bigger on day one.
 *
 * <p>{@link #STRATUM} is the open end of the ladder: past it every further step is a numbered stratum,
 * which is what lets the tiers run on without a maximum. Every other constant covers exactly one step.
 *
 * <p>The names run to the seventeenth step because that is where the barème says a squad can actually
 * get: a run climbing at its calibrated pace of one step a week ends around {@link #CITADEL}, and one
 * clearing nearly every challenge reaches efficiency twenty-one, which is step seventeen. Naming that
 * far means every step a squad can reach is a milestone with a name on it, and the numbered open end
 * is a safety valve rather than the last third of a good run.
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
     * Twelfth step.
     */
    CITADEL,

    /**
     * Thirteenth step: the neighbouring cities have fused into one built-up mass.
     */
    CONURBATION,

    /**
     * Fourteenth step: the region itself is the city.
     */
    MEGAREGION,

    /**
     * Fifteenth step: the city is a single structure rather than a collection of them.
     */
    ARCOLOGY,

    /**
     * Sixteenth step: the built ground has no edge left.
     */
    ECUMENOPOLIS,

    /**
     * Seventeenth step: nothing outside remains to absorb, and the city closes on itself.
     */
    CONTINUUM,

    /**
     * Eighteenth step and every one after it, numbered by {@link ColonyTier#level()}.
     *
     * <p>The one name that takes a number without reading as a repetition: once there is no ground
     * left to spread over, a city grows by stacking, and the stratum is which layer it is on.
     */
    STRATUM
}
