package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Selects the challenge pack assigned to a calendar week, and the challenge assigned to each day.
 */
public interface WeeklyChallengeSelectionService {

    /**
     * Returns the weekly challenge pack for the current week.
     *
     * <p>The pack is created when no complete selection exists yet.</p>
     *
     * @return current weekly challenge pack
     */
    List<WeeklyChallenge> selectCurrentWeekChallenges();

    /**
     * Returns the weekly challenge pack for the requested week.
     *
     * <p>Existing selections are preserved and missing difficulty tiers are
     * completed when possible. Daily selections are never part of the pack.</p>
     *
     * @param weekStart Monday identifying the requested week
     * @return selected weekly challenges
     */
    List<WeeklyChallenge> selectWeekChallenges(
        LocalDate weekStart
    );

    /**
     * Returns every selection a week already owns, weekly pack and daily draws alike, creating none.
     *
     * <p>This is the read-only counterpart of {@link #selectWeekChallenges(LocalDate)}, and the
     * only safe way to reach a past week: selecting would hand a finalized week a brand new pack
     * and rewrite history.
     *
     * @param weekStart Monday identifying the requested week
     * @return the week's selections, empty when it never had any
     */
    List<WeeklyChallenge> findExistingWeekChallenges(
        LocalDate weekStart
    );

    /**
     * Discards the current week's weekly pack and draws a brand new one.
     *
     * <p>The counterpart of {@link #selectWeekChallenges(LocalDate)}, which never replaces what a
     * week already holds. This is the operator's override, for the week whose pack no longer
     * matches the catalogue it was drawn from — a challenge disabled or removed after the draw.
     *
     * <p>Restricted to the week in progress: a past week's pack is what its frozen ranking was
     * earned against, and redrawing it would rewrite history. Daily draws are left alone.
     *
     * <p>Destructive. The progress recorded against the discarded pack goes with it, and cannot be
     * recovered: the challenges it was measured against no longer exist.
     *
     * @return the newly drawn pack
     */
    List<WeeklyChallenge> redrawCurrentWeekChallenges();

    /**
     * Returns the daily challenge of one day, drawing it when the day has none yet.
     *
     * <p>Drawn from the daily pool, common to the whole squad, and never repeated within twenty-one
     * days while the pool allows it.
     *
     * @param day day to draw for
     * @return the day's challenge
     */
    WeeklyChallenge selectDailyChallenge(LocalDate day);

    /**
     * Returns the daily challenge of one day, drawing nothing.
     *
     * @param day day to look up
     * @return the day's challenge when it was drawn
     */
    Optional<WeeklyChallenge> findDailyChallenge(LocalDate day);

    /**
     * Returns the daily challenges drawn over a range of days, oldest first, drawing nothing.
     *
     * @param firstDay first day of the range, inclusive
     * @param lastDay  last day of the range, inclusive
     * @return drawn daily challenges
     */
    List<WeeklyChallenge> findDailyChallenges(LocalDate firstDay, LocalDate lastDay);
}
