package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import java.time.LocalDate;
import java.util.List;

/**
 * Selects the challenge pack assigned to a calendar week.
 */
public interface WeeklyChallengeSelectionService {

    /**
     * Returns the challenge pack for the current UTC week.
     *
     * <p>The pack is created when no complete selection exists yet.</p>
     *
     * @return current weekly challenge pack
     */
    List<WeeklyChallenge> selectCurrentWeekChallenges();

    /**
     * Returns the challenge pack for the requested week.
     *
     * <p>Existing selections are preserved and missing difficulty tiers are
     * completed when possible.</p>
     *
     * @param weekStart Monday identifying the requested week
     * @return selected weekly challenges
     */
    List<WeeklyChallenge> selectWeekChallenges(
        LocalDate weekStart
    );

    /**
     * Returns the challenge pack a week already owns, without creating one.
     *
     * <p>This is the read-only counterpart of {@link #selectWeekChallenges(LocalDate)}, and the
     * only safe way to reach a past week: selecting would hand a finalized week a brand new pack
     * and rewrite history.
     *
     * @param weekStart Monday identifying the requested week
     * @return the week's challenges, empty when it never had a pack
     */
    List<WeeklyChallenge> findExistingWeekChallenges(
        LocalDate weekStart
    );

    /**
     * Discards the current week's challenge pack and draws a brand new one.
     *
     * <p>The counterpart of {@link #selectWeekChallenges(LocalDate)}, which never replaces what a
     * week already holds. This is the operator's override, for the week whose pack no longer
     * matches the catalogue it was drawn from — a challenge disabled or removed after the draw.
     *
     * <p>Restricted to the week in progress: a past week's pack is what its frozen ranking was
     * earned against, and redrawing it would rewrite history.
     *
     * <p>Destructive. The progress recorded against the discarded pack goes with it, and cannot be
     * recovered: the challenges it was measured against no longer exist.
     *
     * @return the newly drawn pack
     */
    List<WeeklyChallenge> redrawCurrentWeekChallenges();
}
