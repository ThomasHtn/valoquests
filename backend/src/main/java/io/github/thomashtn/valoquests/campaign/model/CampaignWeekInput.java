package io.github.thomashtn.valoquests.campaign.model;

import java.time.LocalDate;

/**
 * One week's fight as it stands when its Sunday closes, as the replay engine consumes it.
 *
 * <p>The fight itself is settled before the engine sees it: whether the guardian fell, and how far
 * the squad got if it did not, is a question about the week's matches, not about the base. The
 * engine only turns that answer into people saved and people lost.
 *
 * @param weekIndex        one-based position in the campaign
 * @param settlementDay    Sunday the week is settled on
 * @param guardianHitPoints hit points the guardian opened the week with
 * @param woundedCount     wounded stranded on the planet that week
 * @param damageDealt      damage the roster dealt over the week
 * @param defeated         whether the guardian fell
 * @param challengeRescued wounded the week's challenges brought back, before the group cap
 */
public record CampaignWeekInput(
    int weekIndex,
    LocalDate settlementDay,
    int guardianHitPoints,
    int woundedCount,
    int damageDealt,
    boolean defeated,
    int challengeRescued
) {

    /**
     * Returns how far the squad got on the guardian, as a share of its hit points.
     *
     * <p>One for a guardian that fell, whatever the overkill. It multiplies the extraction and,
     * squared, the losses a surviving guardian inflicts.
     *
     * @return progress between zero and one
     */
    public double progress() {
        if (defeated || guardianHitPoints <= 0) {
            return 1;
        }

        return Math.min(1, (double) damageDealt / guardianHitPoints);
    }
}
