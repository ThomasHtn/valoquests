package io.github.thomashtn.valoquests.challenge.model;

import java.util.Objects;

/**
 * What a challenge is priced against for one week: the reference in force, the position of the week
 * inside its campaign, and the difficulty whose grid is played.
 *
 * @param reference  reference in force, the campaign's or the amateur one outside any campaign
 * @param weekIndex  one-based week index inside the campaign, one outside any campaign
 * @param difficulty difficulty frozen by the campaign, amateur outside any campaign
 */
public record ChallengeCalibration(
    int reference,
    int weekIndex,
    CampaignDifficulty difficulty
) {

    /**
     * Creates a validated calibration.
     */
    public ChallengeCalibration {
        Objects.requireNonNull(difficulty, "Difficulty must not be null.");

        if (reference <= 0) {
            throw new IllegalArgumentException("Reference must be positive.");
        }

        if (weekIndex <= 0) {
            throw new IllegalArgumentException("Week index must be positive.");
        }
    }
}
