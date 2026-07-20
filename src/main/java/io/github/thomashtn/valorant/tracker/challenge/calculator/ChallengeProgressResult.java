package io.github.thomashtn.valorant.tracker.challenge.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Contains the normalized result produced by a challenge calculator.
 *
 * @param currentValue       calculated player progress
 * @param targetValue        value required to complete the challenge
 * @param progressPercentage normalized completion percentage
 * @param completed          whether the target has been reached
 */
public record ChallengeProgressResult(
    BigDecimal currentValue,
    BigDecimal targetValue,
    BigDecimal progressPercentage,
    boolean completed
) {

    /**
     * Maximum percentage displayed for a challenge.
     */
    private static final BigDecimal MAXIMUM_PERCENTAGE =
        BigDecimal.valueOf(100);

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

        BigDecimal percentage = safeCurrentValue
            .multiply(MAXIMUM_PERCENTAGE)
            .divide(targetValue, 2, RoundingMode.HALF_UP)
            .min(MAXIMUM_PERCENTAGE);

        return new ChallengeProgressResult(
            safeCurrentValue,
            targetValue,
            percentage,
            completed
        );
    }
}
