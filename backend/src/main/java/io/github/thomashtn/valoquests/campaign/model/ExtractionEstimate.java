package io.github.thomashtn.valoquests.campaign.model;

import io.github.thomashtn.valoquests.campaign.CampaignRuleset;

/**
 * What one Sunday's extraction would bring home from a given base, and what caps it.
 *
 * <p>Pure arithmetic shared by the replay, which settles a week for real, and the campaign reading,
 * which forecasts the week in progress from the base as it stands: both must agree on the rule or
 * the forecast would promise what the settlement never delivers.
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
     * @param challengeSurvivors  wounded the challenges brought home so far
     * @param food                food in reserve
     * @param components          components in reserve
     * @param population          inhabitants, whose next seven evenings are never spent
     * @param progress            share of the guardian's hit points taken, in [0, 1]
     * @return the estimate
     */
    public static ExtractionEstimate of(
        int woundedCount,
        int challengeSurvivors,
        double food,
        double components,
        double population,
        double progress
    ) {
        int challengeRescued = Math.min(challengeSurvivors, woundedCount);
        int remainingGroup = woundedCount - challengeRescued;

        // The seven evenings ahead are never on the table: without this a squad that plays at the
        // weekend paid Sunday's rescue with the food it needed to survive to Friday.
        double reserve = CampaignRuleset.PROTECTED_FOOD_DAYS * population * CampaignRuleset.FOOD_PER_INHABITANT_PER_DAY;
        int byComponents = (int) Math.floor(components / CampaignRuleset.COMPONENTS_PER_RESCUE);
        int byFood = (int) Math.floor(Math.max(0, food - reserve) / CampaignRuleset.FOOD_PER_RESCUE);
        int reachable = Math.min(remainingGroup, Math.min(byComponents, byFood));
        int extracted = (int) Math.floor(reachable * progress);

        return new ExtractionEstimate(
            challengeRescued,
            remainingGroup,
            byComponents,
            byFood,
            extracted,
            limiterOf(woundedCount, challengeRescued + extracted, remainingGroup, byComponents, byFood)
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
