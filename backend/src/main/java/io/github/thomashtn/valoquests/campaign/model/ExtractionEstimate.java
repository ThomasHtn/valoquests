package io.github.thomashtn.valoquests.campaign.model;

/**
 * What one Sunday's extraction would bring home from a given base, and what caps it.
 *
 * <p>Pure arithmetic shared by the replay, which settles a week for real, and the campaign reading,
 * which forecasts the week in progress from the base as it stands, so the forecast never promises
 * what the settlement does not deliver.
 *
 * @param challengeRescued  wounded the challenges already brought home, capped at the group
 * @param remainingGroup    wounded the ship has to reach itself
 * @param byComponents      wounded the components in reserve could carry
 * @param byFood            wounded the spendable food could settle
 * @param extracted         wounded the ship brings home once the guardian's lines are accounted for
 * @param limiter           what capped the extraction
 */
public record ExtractionEstimate(
    int challengeRescued,
    int remainingGroup,
    int byComponents,
    int byFood,
    int extracted,
    ExtractionLimiter limiter
) {

    /**
     * Estimates one extraction.
     *
     * @param woundedCount        wounded stranded that week
     * @param challengeRescued    wounded the challenges brought home so far, before the group cap
     * @param food                food in reserve
     * @param components          components in reserve
     * @param population          inhabitants, whose next seven evenings are never spent
     * @param progress            share of the guardian's hit points taken, in [0, 1]
     * @return the estimate
     */
    public static ExtractionEstimate of(
        int woundedCount,
        int challengeRescued,
        double food,
        double components,
        double population,
        double progress
    ) {
        int cappedChallengeRescued = Math.min(challengeRescued, woundedCount);
        int remainingGroup = woundedCount - cappedChallengeRescued;

        RescueCapacity capacity = RescueCapacity.of(food, components, population);
        int byComponents = capacity.byComponents();
        int byFood = capacity.byFood();
        int reachable = Math.min(remainingGroup, Math.min(byComponents, byFood));
        int extracted = (int) Math.floor(reachable * progress);

        return new ExtractionEstimate(
            cappedChallengeRescued,
            remainingGroup,
            byComponents,
            byFood,
            extracted,
            limiterOf(woundedCount, cappedChallengeRescued + extracted, remainingGroup, byComponents, byFood)
        );
    }

    /**
     * Returns the wounded brought home altogether, challenges and ship.
     *
     * @return the rescued
     */
    public int rescued() {
        return challengeRescued + extracted;
    }

    /**
     * Names what capped the extraction: nothing, the group left, or the stock that ran out first.
     */
    private static ExtractionLimiter limiterOf(
        int woundedCount,
        int rescued,
        int remainingGroup,
        int byComponents,
        int byFood
    ) {
        if (rescued >= woundedCount) {
            return ExtractionLimiter.NONE;
        }

        if (remainingGroup <= byComponents && remainingGroup <= byFood) {
            return ExtractionLimiter.GROUP;
        }

        return byComponents <= byFood ? ExtractionLimiter.COMPONENTS : ExtractionLimiter.FOOD;
    }
}
