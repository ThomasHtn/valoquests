package io.github.thomashtn.valoquests.campaign.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The base at the close of one week, and what the week added to its stocks.
 *
 * <p>For the week in progress the figures stop at the last replayed day.
 *
 * @param population       inhabitants on the week's last replayed day
 * @param populationChange inhabitants gained or lost since the previous week's close
 * @param foodStock        food in reserve on that day, after Sunday's spending once settled
 * @param componentsStock  components in reserve on that day
 * @param foodGained       food the week brought in
 * @param componentsGained components the week brought in
 */
@Schema(description = "The base at the close of one week and what the week added.")
public record CampaignWeekBaseResponse(
    int population,
    int populationChange,
    int foodStock,
    int componentsStock,
    int foodGained,
    int componentsGained
) {
}
