package io.github.thomashtn.valoquests.campaign.model;

import java.time.LocalDate;

/**
 * One week's fight as it stands when its Sunday closes, as the replay engine consumes it.
 *
 * <p>The fight is settled from the matches beforehand; the engine only turns it into people saved
 * and lost.
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
     * <p>One for a guardian that fell. It scales the extraction and, squared, the guardian's losses.
     *
     * @return progress between zero and one
     */
    public double progress() {
        return GuardianProgress.of(defeated, damageDealt, guardianHitPoints);
    }
}
