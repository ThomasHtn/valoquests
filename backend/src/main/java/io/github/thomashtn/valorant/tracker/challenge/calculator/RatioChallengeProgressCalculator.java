package io.github.thomashtn.valorant.tracker.challenge.calculator;

import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeCondition;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeMetric;
import io.github.thomashtn.valorant.tracker.challenge.model.ProgressMode;
import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Calculates ratio-based weekly challenges.
 *
 * <p>The current challenge catalogue only uses this mode for the global
 * kill-to-death ratio. The ratio is calculated from the total number of kills
 * and deaths across all eligible matches, rather than from an average of
 * per-match ratios.</p>
 */
@Component
public class RatioChallengeProgressCalculator
    implements ChallengeProgressCalculator {

    /**
     * Scale used when calculating ratio values.
     */
    private static final int RATIO_SCALE = 4;

    /**
     * Applies filters declared by challenge conditions.
     */
    private final ChallengeMatchFilter matchFilter;

    /**
     * Creates the ratio challenge-progress calculator.
     *
     * @param matchFilter condition match filter
     */
    public RatioChallengeProgressCalculator(
        ChallengeMatchFilter matchFilter
    ) {
        this.matchFilter = matchFilter;
    }

    /**
     * Returns the progress mode supported by this calculator.
     *
     * @return {@link ProgressMode#RATIO}
     */
    @Override
    public ProgressMode supportedMode() {
        return ProgressMode.RATIO;
    }

    /**
     * Calculates the ratio configured by the challenge definition.
     *
     * <p>A ratio challenge may declare a minimum number of eligible matches.
     * The calculated ratio remains visible before this minimum is reached,
     * but the challenge cannot be completed until the match requirement is
     * satisfied.</p>
     *
     * @param definition parsed challenge definition
     * @param context    weekly player context
     * @return normalized progress result
     */
    @Override
    public ChallengeProgressResult calculate(
        ChallengeDefinition definition,
        PlayerChallengeContext context
    ) {
        ChallengeCondition condition = definition.singleCondition();

        validateCondition(condition);

        List<PlayerMatch> eligibleMatches = context.playerMatches()
            .stream()
            .filter(playerMatch ->
                matchFilter.matches(playerMatch, condition)
            )
            .toList();

        BigDecimal currentValue = calculateRatio(
            condition.metric(),
            eligibleMatches
        );

        ChallengeProgressResult normalizedResult =
            ChallengeProgressResult.from(
                currentValue,
                condition.target()
            );

        boolean minimumMatchesReached =
            eligibleMatches.size() >= condition.minimumMatches();

        return new ChallengeProgressResult(
            normalizedResult.currentValue(),
            normalizedResult.targetValue(),
            normalizedResult.progressPercentage(),
            normalizedResult.completed() && minimumMatchesReached
        );
    }

    /**
     * Validates the configuration required by a ratio challenge.
     *
     * @param condition challenge condition
     */
    private void validateCondition(
        ChallengeCondition condition
    ) {
        if (condition.metric() != ChallengeMetric.KD) {
            throw new IllegalArgumentException(
                "Unsupported ratio metric: " + condition.metric()
            );
        }

        if (condition.minimumMatches() == null) {
            throw new IllegalArgumentException(
                "RATIO challenges require a minimum number of matches."
            );
        }

        if (condition.minimumMatches() <= 0) {
            throw new IllegalArgumentException(
                "The minimum number of matches must be greater than zero."
            );
        }
    }

    /**
     * Calculates the requested ratio.
     *
     * @param metric          configured ratio metric
     * @param eligibleMatches matches included in the calculation
     * @return calculated ratio
     */
    private BigDecimal calculateRatio(
        ChallengeMetric metric,
        List<PlayerMatch> eligibleMatches
    ) {
        return switch (metric) {
            case KD -> calculateKillDeathRatio(eligibleMatches);
            default -> throw new IllegalArgumentException(
                "Unsupported ratio metric: " + metric
            );
        };
    }

    /**
     * Calculates the global kill-to-death ratio.
     *
     * <p>The total number of kills is divided by the total number of deaths.
     * When no death is recorded, the total number of kills is used as the
     * ratio value to avoid division by zero while preserving meaningful
     * progress.</p>
     *
     * @param eligibleMatches matches included in the calculation
     * @return global kill-to-death ratio
     */
    private BigDecimal calculateKillDeathRatio(
        List<PlayerMatch> eligibleMatches
    ) {
        long totalKills = eligibleMatches
            .stream()
            .mapToLong(PlayerMatch::getKills)
            .sum();

        long totalDeaths = eligibleMatches
            .stream()
            .mapToLong(PlayerMatch::getDeaths)
            .sum();

        if (totalDeaths == 0) {
            return BigDecimal.valueOf(totalKills);
        }

        return BigDecimal.valueOf(totalKills)
            .divide(
                BigDecimal.valueOf(totalDeaths),
                RATIO_SCALE,
                RoundingMode.HALF_UP
            );
    }
}
