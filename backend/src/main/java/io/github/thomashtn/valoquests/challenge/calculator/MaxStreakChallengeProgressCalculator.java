package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.challenge.model.ChallengeOperator;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScope;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Calculates the longest consecutive sequence of eligible matches satisfying
 * a per-match challenge condition.
 *
 * <p>Matches are evaluated chronologically. Matches outside the configured
 * game mode are ignored and therefore do not interrupt the sequence.</p>
 */
@Component
public class MaxStreakChallengeProgressCalculator
    implements ChallengeProgressCalculator {

    /**
     * Evaluates the configured metric for individual matches.
     */
    private final ChallengeMetricEvaluator metricEvaluator;

    /**
     * Applies the common game-mode filters.
     */
    private final ChallengeMatchFilter matchFilter;

    /**
     * Creates the maximum-streak calculator.
     *
     * @param metricEvaluator metric evaluator
     * @param matchFilter     condition match filter
     */
    public MaxStreakChallengeProgressCalculator(
        ChallengeMetricEvaluator metricEvaluator,
        ChallengeMatchFilter matchFilter
    ) {
        this.metricEvaluator = metricEvaluator;
        this.matchFilter = matchFilter;
    }

    /**
     * Returns the supported progress mode.
     *
     * @return {@link ProgressMode#MAX_STREAK}
     */
    @Override
    public ProgressMode supportedMode() {
        return ProgressMode.MAX_STREAK;
    }

    /**
     * Calculates the longest chronological sequence of matches satisfying the
     * configured condition.
     *
     * @param definition parsed challenge definition
     * @param context    weekly player context
     * @return normalized streak progress
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
            .sorted(
                Comparator.comparing(
                    playerMatch ->
                        playerMatch.getMatch().getStartedAt()
                )
            )
            .toList();

        int maximumStreak = calculateMaximumStreak(
            eligibleMatches,
            condition
        );

        return ChallengeProgressResult.from(
            BigDecimal.valueOf(maximumStreak),
            BigDecimal.valueOf(condition.streak())
        );
    }

    /**
     * Calculates the longest consecutive sequence satisfying the condition.
     *
     * @param eligibleMatches chronologically ordered eligible matches
     * @param condition       challenge condition
     * @return maximum consecutive-match count
     */
    private int calculateMaximumStreak(
        List<PlayerMatch> eligibleMatches,
        ChallengeCondition condition
    ) {
        int currentStreak = 0;
        int maximumStreak = 0;

        for (PlayerMatch playerMatch : eligibleMatches) {
            if (matchesCondition(playerMatch, condition)) {
                currentStreak++;
                maximumStreak = Math.max(
                    maximumStreak,
                    currentStreak
                );
            } else {
                currentStreak = 0;
            }
        }

        return maximumStreak;
    }

    /**
     * Determines whether one eligible match satisfies the configured metric
     * threshold.
     *
     * @param playerMatch player-match statistics
     * @param condition   challenge condition
     * @return {@code true} when the match continues the sequence
     */
    private boolean matchesCondition(
        PlayerMatch playerMatch,
        ChallengeCondition condition
    ) {
        BigDecimal currentValue = metricEvaluator.evaluate(
            playerMatch,
            condition.metric()
        );

        if (condition.operator() == ChallengeOperator.GTE) {
            return currentValue.compareTo(condition.target()) >= 0;
        }

        throw new IllegalArgumentException(
            "Unsupported challenge operator: "
                + condition.operator()
        );
    }

    /**
     * Validates the configuration required by a streak challenge.
     *
     * @param condition challenge condition
     */
    private void validateCondition(
        ChallengeCondition condition
    ) {
        if (condition.scope() != ChallengeScope.PER_MATCH) {
            throw new IllegalArgumentException(
                "MAX_STREAK challenges require the PER_MATCH scope."
            );
        }

        if (condition.streak() == null || condition.streak() <= 0) {
            throw new IllegalArgumentException(
                "MAX_STREAK challenges require a positive streak value."
            );
        }

        if (condition.metric() == ChallengeMetric.PLAY_DAY) {
            throw new IllegalArgumentException(
                "PLAY_DAY cannot be evaluated by MAX_STREAK."
            );
        }
    }
}
