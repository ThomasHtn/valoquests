package io.github.thomashtn.valoquests.challenge.calculator;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Contains the normalized result produced by a challenge calculator.
 *
 * @param currentValue calculated player progress
 * @param targetValue  value required to complete the challenge
 * @param completed    whether the target has been reached
 */
public record ChallengeProgressResult(

    BigDecimal currentValue,
    BigDecimal targetValue,
    boolean completed
) {

    /**
     * Creates a normalized challenge progress result.
     *
     * @param currentValue calculated value
     * @param targetValue  target value
     * @return normalized result
     */
    public static ChallengeProgressResult from(
        BigDecimal currentValue,
        BigDecimal targetValue
    ) {
        Objects.requireNonNull(
            currentValue,
            "Current value must not be null."
        );
        Objects.requireNonNull(
            targetValue,
            "Target value must not be null."
        );

        if (targetValue.signum() <= 0) {
            throw new IllegalArgumentException(
                "Challenge target value must be greater than zero."
            );
        }

        BigDecimal safeCurrentValue = currentValue.max(BigDecimal.ZERO);
        boolean completed =
            safeCurrentValue.compareTo(targetValue) >= 0;

        return new ChallengeProgressResult(
            safeCurrentValue,
            targetValue,
            completed
        );
    }
}
