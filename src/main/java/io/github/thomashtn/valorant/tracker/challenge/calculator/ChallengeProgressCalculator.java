package io.github.thomashtn.valorant.tracker.challenge.calculator;

import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valorant.tracker.challenge.model.ProgressMode;

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
}
