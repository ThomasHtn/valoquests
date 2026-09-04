package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCalibration;
import java.time.LocalDate;

/**
 * Says what challenges are priced and scaled against for one week.
 *
 * <p>The campaign package implements it from the campaign covering the week. Outside any campaign
 * the reference is the floor, the week index one and the scaling neutral.
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
