package io.github.thomashtn.valoquests.campaign.model;

import java.time.LocalDate;

/**
 * What one player contributed to the squad's reference, and how trustworthy it is.
 *
 * <p>Kept per player so the backoffice can show the calibration before committing to it. A
 * reference is decided once and never revised, so the operator has exactly one chance to notice
 * that a player's history is thinner than it looks.
 *
 * @param playerId          internal player identifier
 * @param displayName       player's Riot name, for the preview screen
 * @param weeklyAverage     average weekly damage over the window, empty weeks counted as zero
 * @param weeksCounted      weeks the average was divided by
 * @param earliestMatchDay  day of the player's oldest known match, or {@code null} without any
 * @param covered           whether the known history reaches back past the window's first day
 * @param beginner          whether the player has under a month of history and took the squad median
 */
public record PlayerCalibration(
    long playerId,
    String displayName,
    int weeklyAverage,
    int weeksCounted,
    LocalDate earliestMatchDay,
    boolean covered,
    boolean beginner
) {
}
