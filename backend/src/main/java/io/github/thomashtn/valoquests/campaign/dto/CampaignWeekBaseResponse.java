package io.github.thomashtn.valoquests.campaign.dto;

/**
 * The base at the close of one week, and what the week added to its stocks.
 *
 * <p>The ledger of the campaign page is read from this: what a week brought in, what its Sunday
 * spent (carried by the week itself) and what was left over for the next one. For the week in
 * progress the figures stop at the last replayed day.
 *
 * @param population       inhabitants on the week's last replayed day
 * @param populationChange inhabitants gained or lost since the previous week's close
 * @param foodStock        food in reserve on that day, after Sunday's spending once settled
 * @param componentsStock  components in reserve on that day
 * @param foodGained       food the week brought in
 * @param componentsGained components the week brought in
 */
public record CampaignWeekBaseResponse(
    int population,
    int populationChange,
    int foodStock,
    int componentsStock,
    int foodGained,
    int componentsGained
) {
}
