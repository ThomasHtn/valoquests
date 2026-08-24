package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import java.util.OptionalInt;

/**
 * Calculates player progress for one supported challenge progress mode.
 */
public interface ChallengeProgressCalculator {

    /**
     * Returns the progress mode handled by this calculator.
     *
     * @return supported progress mode
     */
    ProgressMode supportedMode();

    /**
     * Calculates the progress of one challenge from the supplied weekly
     * context.
     *
     * @param definition parsed challenge definition
     * @param context    weekly player context
     * @return normalized challenge progress
     */
    ChallengeProgressResult calculate(
        ChallengeDefinition definition,
        PlayerChallengeContext context
    );

    /**
     * Finds the chronological match at which this challenge was first completed.
     *
     * <p>Replays {@link #calculate} over growing chronological prefixes of {@link
     * PlayerChallengeContext#playerMatches()} rather than duplicating each calculator's aggregation
     * logic. This is what lets the weekly-boss finishing blow be attributed to the exact match that
     * unlocked a challenge, for every progress mode, without touching any calculator implementation.
     *
     * <p>First completion, not sustained completion: a challenge is latched the moment it is reached
     * and can no longer be lost (see {@code PlayerChallengeProgressPersistenceService}). Most progress
     * modes are monotonic and never could be; the kill-to-death ratio is the one that can fall back
     * below its target as more matches are summed, and latching is precisely what stops it from
     * punishing a player for continuing to play. This method has to latch the same way, or the ranking
     * would credit a completion the boss chronology says never happened.
     *
     * @param definition parsed challenge definition
     * @param context    weekly player context, matches assumed chronologically ordered
     * @return zero-based index into {@link PlayerChallengeContext#playerMatches()} of the match that
     *     first completed the challenge, or empty when it never did
     */
    default OptionalInt findFirstCompletionIndex(
        ChallengeDefinition definition,
        PlayerChallengeContext context
    ) {
        int matchCount = context.playerMatches().size();

        for (int index = 0; index < matchCount; index++) {
            boolean completed = calculate(
                definition,
                context.prefixedTo(index + 1)
            ).completed();

            if (completed) {
                return OptionalInt.of(index);
            }
        }

        return OptionalInt.empty();
    }
}
