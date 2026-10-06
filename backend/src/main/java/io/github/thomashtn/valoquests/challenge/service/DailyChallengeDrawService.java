package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import java.time.LocalDate;
import java.util.List;

/**
 * Draws the challenge assigned to each day, and reads the days already drawn.
 */
public interface DailyChallengeDrawService {

    /**
     * Returns the daily challenge of one day, drawing it when the day has none yet.
     *
     * <p>Drawn from the daily pool, common to the whole squad, and never repeated within twenty-seven
     * days while the pool allows it.
     *
     * @param day day to draw for
     * @return the day's challenge
     */
    ChallengeSelection selectDailyChallenge(LocalDate day);

    /**
     * Returns the daily challenges drawn over a range of days, oldest first, drawing nothing.
     *
     * @param firstDay first day of the range, inclusive
     * @param lastDay  last day of the range, inclusive
     * @return drawn daily challenges
     */
    List<ChallengeSelection> findDailyChallenges(LocalDate firstDay, LocalDate lastDay);
}
