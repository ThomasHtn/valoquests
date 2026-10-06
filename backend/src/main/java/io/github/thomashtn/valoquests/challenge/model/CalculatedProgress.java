package io.github.thomashtn.valoquests.challenge.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressResult;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import java.util.Objects;

/**
 * One selection paired with the progress a player was just calculated to have on it.
 *
 * @param selection evaluated selection
 * @param result    progress calculated against it
 */
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Carries the managed selection entity to the writer that attaches progress to it."
)
public record CalculatedProgress(ChallengeSelection selection, ChallengeProgressResult result) {

    /**
     * Creates the pair, rejecting a missing half.
     */
    public CalculatedProgress {
        Objects.requireNonNull(selection, "Selection must not be null.");
        Objects.requireNonNull(result, "Challenge progress result must not be null.");
    }
}
