package io.github.thomashtn.valorant.tracker.challenge.calculator;

import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeCondition;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeGroupBy;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeMetric;
import io.github.thomashtn.valorant.tracker.challenge.model.ProgressMode;
import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Calculates challenges whose progress is the number of distinct values found
 * across eligible weekly matches.
 */
@Component
public class DistinctCountChallengeProgressCalculator
    implements ChallengeProgressCalculator {

    /**
     * Evaluates whether a match contributes to the configured metric.
     */
    private final ChallengeMetricEvaluator metricEvaluator;

    /**
     * Applies filters declared by challenge conditions.
     */
    private final ChallengeMatchFilter matchFilter;

    /**
     * Creates the distinct-value calculator.
     *
     * @param metricEvaluator metric evaluator
     * @param matchFilter     condition match filter
     */
    public DistinctCountChallengeProgressCalculator(
        ChallengeMetricEvaluator metricEvaluator,
        ChallengeMatchFilter matchFilter
    ) {
        this.metricEvaluator = metricEvaluator;
        this.matchFilter = matchFilter;
    }

    /**
     * Returns the supported progress mode.
     *
     * @return {@link ProgressMode#DISTINCT_COUNT}
     */
    @Override
    public ProgressMode supportedMode() {
        return ProgressMode.DISTINCT_COUNT;
    }

    /**
     * Counts distinct grouping values among matches contributing to the
     * configured metric.
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
        ChallengeGroupBy groupBy = condition.groupBy();

        if (groupBy == null) {
            throw new IllegalArgumentException(
                "DISTINCT_COUNT challenges require a grouping dimension."
            );
        }

        long distinctValues = context.playerMatches()
            .stream()
            .filter(playerMatch ->
                matchFilter.matches(playerMatch, condition)
            )
            .filter(playerMatch -> contributes(playerMatch, condition))
            .map(playerMatch -> extractGroupValue(playerMatch, groupBy))
            .filter(Objects::nonNull)
            .distinct()
            .count();

        return ChallengeProgressResult.from(
            BigDecimal.valueOf(distinctValues),
            condition.target()
        );
    }

    /**
     * Determines whether one eligible match contributes to the distinct set.
     *
     * <p>The play-day metric is represented by the grouping key itself. Other
     * metrics must produce a strictly positive value, which notably excludes
     * losses from challenges based on {@code MATCHES_WON}.</p>
     *
     * @param playerMatch persisted player-match data
     * @param condition   parsed challenge condition
     * @return {@code true} when the match contributes to progress
     */
    private boolean contributes(
        PlayerMatch playerMatch,
        ChallengeCondition condition
    ) {
        if (condition.metric() == ChallengeMetric.PLAY_DAY) {
            return true;
        }

        return metricEvaluator
            .evaluate(playerMatch, condition.metric())
            .signum() > 0;
    }

    /**
     * Extracts the stable value used to group one player match.
     *
     * @param playerMatch persisted player-match data
     * @param groupBy     requested grouping dimension
     * @return grouping value, or {@code null} when unavailable
     */
    private Object extractGroupValue(
        PlayerMatch playerMatch,
        ChallengeGroupBy groupBy
    ) {
        return switch (groupBy) {
            case AGENT -> extractAgentValue(playerMatch);
            case GAME_MODE -> playerMatch.getMatch().getGameMode();
            case PLAY_DAY -> playerMatch
                .getMatch()
                .getStartedAt()
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
        };
    }

    /**
     * Returns the most stable available agent identifier.
     *
     * @param playerMatch persisted player-match data
     * @return agent identifier or fallback name
     */
    private String extractAgentValue(PlayerMatch playerMatch) {
        if (playerMatch.getAgentId() != null
            && !playerMatch.getAgentId().isBlank()) {
            return playerMatch.getAgentId();
        }

        if (playerMatch.getAgentName() != null
            && !playerMatch.getAgentName().isBlank()) {
            return playerMatch.getAgentName();
        }

        return null;
    }
}
