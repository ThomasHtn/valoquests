package io.github.thomashtn.valoquests.campaign.model;

import io.github.thomashtn.valoquests.campaign.CampaignRuleset;

/**
 * How many wounded a base's stocks could bring home on a Sunday, stock by stock.
 *
 * @param protectedFood food the extraction never touches
 * @param byComponents  wounded the components in reserve could reach
 * @param byFood        wounded the spendable food could settle
 */
public record RescueCapacity(double protectedFood, int byComponents, int byFood) {

    /**
     * Measures the capacity of one base.
     *
     * @param food       food in reserve
     * @param components components in reserve
     * @param population inhabitants, whose next seven evenings are never spent
     * @return the capacity
     */
    public static RescueCapacity of(double food, double components, double population) {
        // Protected so Sunday's rescue never spends the food the base needs to survive the next week.
        double protectedFood = CampaignRuleset.protectedFood(population);

        return new RescueCapacity(
            protectedFood,
            (int) Math.floor(components / CampaignRuleset.COMPONENTS_PER_RESCUE),
            (int) Math.floor(Math.max(0, food - protectedFood) / CampaignRuleset.FOOD_PER_RESCUE)
        );
    }
}
