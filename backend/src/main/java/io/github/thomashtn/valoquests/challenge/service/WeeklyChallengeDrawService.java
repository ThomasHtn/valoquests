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
     * <p>Existing selections are preserved and missing tiers are
     * completed when possible. Daily selections are never part of the pack.</p>
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
     * <p>This is the read-only counterpart of {@link #selectWeekChallenges(LocalDate)}, and the
     * only safe way to reach a past week: selecting would hand a finalized week a brand new pack
     * and rewrite history.
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
     * <p>The counterpart of {@link #selectWeekChallenges(LocalDate)}, which never replaces what a
     * week already holds. This is the admin's override, for the week whose pack no longer
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
    List<ChallengeSelection> redrawCurrentWeekChallenges();
}
