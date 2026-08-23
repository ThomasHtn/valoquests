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
     * Finds the chronological match at which this challenge became sustainably completed.
     *
     * <p>Replays {@link #calculate} over growing chronological prefixes of {@link
     * PlayerChallengeContext#playerMatches()} rather than duplicating each calculator's aggregation
     * logic. This is what lets the weekly-boss finishing blow be attributed to the exact match that
     * unlocked a challenge, for every progress mode, without touching any calculator implementation.
     *
     * <p>Most progress modes are monotonic: once the target is reached, adding more matches never
     * un-reaches it, so the first match at which {@code completed} turns {@code true} is also the only
     * one. The kill-to-death ratio challenge is the one exception in this catalogue — the running ratio
     * can cross the target and later fall back below it as more matches are summed. "Sustained
     * completion" is therefore defined as the last match at which {@code completed} turns {@code true}
     * and never turns {@code false} again before the end of the supplied matches: unambiguous, and
     * consistent with the fact that {@code completed} is already allowed to flip back to {@code false}
     * elsewhere in this codebase (see {@code PlayerChallengeProgressPersistenceService}).
     *
     * @param definition parsed challenge definition
     * @param context    weekly player context, matches assumed chronologically ordered
     * @return zero-based index into {@link PlayerChallengeContext#playerMatches()} of the match that
     *     sustainably completed the challenge, or empty when it never did
     */
    default OptionalInt findSustainedCompletionIndex(
        ChallengeDefinition definition,
        PlayerChallengeContext context
    ) {
        int matchCount = context.playerMatches().size();
        int sustainedIndex = -1;

        for (int index = 0; index < matchCount; index++) {
            boolean completed = calculate(
                definition,
                context.prefixedTo(index + 1)
            ).completed();

            if (completed) {
                if (sustainedIndex == -1) {
                    sustainedIndex = index;
                }
            } else {
                sustainedIndex = -1;
            }
        }

        return sustainedIndex < 0 ? OptionalInt.empty() : OptionalInt.of(sustainedIndex);
    }
}
