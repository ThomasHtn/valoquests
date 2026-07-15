package io.github.thomashtn.valorant.tracker.challenge.service;

/** Defines the manual challenge-progress recalculation operation. */
public interface ChallengeRecalculationService {

    /** Recalculates active-week progress and then updates the current ranking. */
    void recalculateCurrentWeekProgress();
}
