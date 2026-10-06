package io.github.thomashtn.valoquests.campaign.model;

import java.time.LocalDate;

/**
 * What the frozen roster produced on one calendar day, as the replay engine consumes it.
 *
 * <p>Already priced, both multipliers applied. Days nobody played are present with zeroes: the base
 * still eats on them.
 *
 * @param day            calendar day
 * @param damage         damage every roster player dealt that day, food and components summed
 * @param food           food produced that day
 * @param components     components produced that day
 * @param presenceCount  roster players who played at least one valued match that day
 */
public record CampaignDayInput(
    LocalDate day,
    int damage,
    int food,
    int components,
    int presenceCount
) {
}
