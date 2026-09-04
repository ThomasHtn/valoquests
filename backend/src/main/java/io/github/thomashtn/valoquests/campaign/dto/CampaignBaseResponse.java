package io.github.thomashtn.valoquests.campaign.dto;

/**
 * The base as it stands right now: its size, its two stocks and what they can pay for.
 *
 * <p>The capacities are the point. A stock only means something next to what it buys, and the whole
 * arbitrage of a week is reading "components reach 84, food settles 61" and knowing which one to go
 * and get.
 *
 * @param population           inhabitants
 * @param foodStock            food in reserve
 * @param componentsStock      components in reserve
 * @param dailyUpkeep          food the base will eat this evening
 * @param protectedFood        food the ship may never spend, seven evenings of upkeep
 * @param rescuesByComponents  wounded the components in reserve could reach
 * @param rescuesByFood        wounded the spendable food could settle
 */
public record CampaignBaseResponse(
    int population,
    int foodStock,
    int componentsStock,
    int dailyUpkeep,
    int protectedFood,
    int rescuesByComponents,
    int rescuesByFood
) {
}
