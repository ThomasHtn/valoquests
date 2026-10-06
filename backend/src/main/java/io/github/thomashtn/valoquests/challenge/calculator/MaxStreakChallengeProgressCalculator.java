package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
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
    extends SingleConditionChallengeProgressCalculator {

    /**
     * Evaluates the configured metric for individual matches.
     */
    private final ChallengeMetricEvaluator metricEvaluator;

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
        super(matchFilter);
        this.metricEvaluator = metricEvaluator;
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
     * @param condition       the challenge's single condition
     * @param eligibleMatches matches the condition's filters accept
     * @return longest streak
     */
    @Override
    protected BigDecimal measure(ChallengeCondition condition, List<PlayerMatch> eligibleMatches) {
        List<PlayerMatch> chronologicalMatches = eligibleMatches.stream()
            .sorted(Comparator.comparing(playerMatch -> playerMatch.getMatch().getStartedAt()))
            .toList();

        return BigDecimal.valueOf(calculateMaximumStreak(chronologicalMatches, condition));
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
        return condition.isMetBy(metricEvaluator.evaluate(
            playerMatch,
            condition.metric()
        ));
    }
}
