package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.scoring.model.ChallengeCalibration;
import java.time.LocalDate;

/**
 * Says which reference, grid and reward progression a week's challenges are priced against.
 *
 * <p>The campaign package implements it from the campaign covering the week.
 */
public interface ChallengeCalibrationSource {

    /**
     * Returns the calibration in force for one week.
     *
     * @param weekStart Monday identifying the week
     * @return calibration the week's challenges are drawn against
     */
    ChallengeCalibration forWeek(LocalDate weekStart);
}
