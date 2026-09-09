package io.github.thomashtn.valoquests.challenge.model;

import java.util.Objects;

/**
 * What a challenge is priced against for one week: the reference in force, the position of the week
 * inside its campaign, and the squad level whose grid is played.
 *
 * @param reference reference in force, the campaign's or the floor outside any campaign
 * @param weekIndex one-based week index inside the campaign, one outside any campaign
 * @param level     squad level frozen by the campaign, reference outside any campaign
 */
public record ChallengeCalibration(
    int reference,
    int weekIndex,
    SquadLevel level
) {

    /**
     * Creates a validated calibration.
     */
    public ChallengeCalibration {
        Objects.requireNonNull(level, "Squad level must not be null.");

        if (reference <= 0) {
            throw new IllegalArgumentException("Reference must be positive.");
        }

        if (weekIndex <= 0) {
            throw new IllegalArgumentException("Week index must be positive.");
        }
    }
}
