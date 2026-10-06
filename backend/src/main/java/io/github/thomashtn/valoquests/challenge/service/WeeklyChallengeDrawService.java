package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import java.time.LocalDate;
import java.util.List;

/**
 * Draws the challenge pack assigned to a calendar week, and reads the selections a week owns.
 */
public interface WeeklyChallengeDrawService {

    /**
     * Returns the weekly challenge pack for the requested week.
     *
     * <p>Existing selections are kept and missing tiers completed when possible; dailies are never part of it.
     *
     * @param weekStart Monday identifying the requested week
     * @return selected weekly challenges
     */
    List<ChallengeSelection> selectWeekChallenges(
        LocalDate weekStart
    );

    /**
     * Returns every selection a week already owns, weekly pack and daily draws alike, creating none.
     *
     * <p>The only safe way to read a past week: {@link #selectWeekChallenges(LocalDate)} would give it a new pack.
     *
     * @param weekStart Monday identifying the requested week
     * @return the week's selections, empty when it never had any
     */
    List<ChallengeSelection> findExistingWeekChallenges(
        LocalDate weekStart
    );

    /**
     * Discards the current week's weekly pack and draws a brand new one.
     *
     * <p>Destructive and limited to the week in progress: the discarded pack's progress is lost for good.
     * Daily draws are left alone.
     *
     * @return the newly drawn pack
     */
    List<ChallengeSelection> redrawCurrentWeekChallenges();
}
