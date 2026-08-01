package io.github.thomashtn.valorant.tracker.challenge.service;

import io.github.thomashtn.valorant.tracker.challenge.entity.WeeklyChallenge;
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
}
