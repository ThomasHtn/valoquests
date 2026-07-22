package io.github.thomashtn.valorant.tracker.challenge.model;

import java.math.BigDecimal;

/**
 * Represents one typed condition extracted from a challenge JSON definition.
 *
 * @param metric         statistic evaluated by the condition
 * @param operator       comparison operator applied to the calculated value
 * @param target         target value required by the condition
 * @param gameMode       optional game-mode filter
 * @param groupBy        optional grouping dimension
 * @param scope          optional evaluation scope
 * @param occurrences    optional number of matching occurrences required
 * @param streak         optional consecutive-match target
 * @param minimumMatches optional minimum number of eligible matches
 */
public record ChallengeCondition(

    ChallengeMetric metric,
    ChallengeOperator operator,
    BigDecimal target,
    ChallengeGameMode gameMode,
    ChallengeGroupBy groupBy,
    ChallengeScope scope,
    Integer occurrences,
    Integer streak,
    Integer minimumMatches
) {

    /**
     * Returns the effective game-mode filter.
     *
     * <p>Definitions without an explicit game mode are treated as applying to
     * every game mode.</p>
     *
     * @return configured mode or {@link ChallengeGameMode#ANY}
     */
    public ChallengeGameMode effectiveGameMode() {
        return gameMode == null ? ChallengeGameMode.ANY : gameMode;
    }

    /**
     * Determines whether this condition is evaluated independently per match.
     *
     * @return {@code true} when the condition uses the per-match scope
     */
    public boolean isPerMatch() {
        return scope == ChallengeScope.PER_MATCH;
    }
}
