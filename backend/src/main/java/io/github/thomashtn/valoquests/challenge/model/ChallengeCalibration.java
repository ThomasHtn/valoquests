package io.github.thomashtn.valoquests.challenge.model;

import java.util.Objects;

/**
 * What a challenge is priced and scaled against for one week: the reference in force, the position
 * of the week inside its campaign, and the target scaling.
 *
 * @param reference reference in force, the campaign's or the floor outside any campaign
 * @param weekIndex one-based week index inside the campaign, one outside any campaign
 * @param scaling   target scaling frozen by the campaign, or none
 */
public record ChallengeCalibration(
    int reference,
    int weekIndex,
    ChallengeScaling scaling
) {

    /**
     * Creates a validated calibration.
     */
    public ChallengeCalibration {
        Objects.requireNonNull(scaling, "Challenge scaling must not be null.");

        if (reference <= 0) {
            throw new IllegalArgumentException("Reference must be positive.");
        }

        if (weekIndex <= 0) {
            throw new IllegalArgumentException("Week index must be positive.");
        }
    }
}
