package io.github.thomashtn.valoquests.campaign.model;

import java.time.LocalDate;

/**
 * What the frozen roster produced on one calendar day, as the replay engine consumes it.
 *
 * <p>Already priced: both multipliers were applied by the scoring reader before the day reached
 * here, so the engine only ever adds, spends and feeds. Days nobody played are still present, with
 * zeroes — they are the days the base eats without earning, which is the whole point of the famine
 * rule.
 *
 * @param day            calendar day
 * @param damage         damage every roster operator dealt that day, food and components summed
 * @param food           food produced that day
 * @param components     components produced that day
 * @param presenceCount  roster operators who played at least one valued match that day
 */
public record CampaignDayInput(
    LocalDate day,
    int damage,
    int food,
    int components,
    int presenceCount
) {
}
