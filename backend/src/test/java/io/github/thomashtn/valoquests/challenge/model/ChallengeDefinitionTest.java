package io.github.thomashtn.valoquests.challenge.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the derived views of {@link ChallengeDefinition}.
 */
class ChallengeDefinitionTest {

    /**
     * Verifies that the progress target agrees with what each calculator compares against.
     */
    @Test
    void shouldExposeTheTargetCalculatorsCompareAgainst() {
        ChallengeCondition bar = condition(ChallengeMetric.KILLS, 20, ChallengeGameMode.COMPETITIVE, 5, null);

        assertThat(new ChallengeDefinition(3, ProgressMode.COUNT_MATCHES, List.of(bar)).progressTarget())
            .isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(new ChallengeDefinition(3, ProgressMode.MAX_STREAK, List.of(
            condition(ChallengeMetric.MATCHES_WON, 1, ChallengeGameMode.COMPETITIVE, null, 3)
        )).progressTarget()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(new ChallengeDefinition(3, ProgressMode.ALL, List.of(
            condition(ChallengeMetric.MATCHES_PLAYED, 6, ChallengeGameMode.DEATHMATCH, null, null),
            condition(ChallengeMetric.MATCHES_PLAYED, 4, ChallengeGameMode.TEAM_DEATHMATCH, null, null)
        )).progressTarget()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(new ChallengeDefinition(3, ProgressMode.SUM, List.of(
            condition(ChallengeMetric.KILLS, 60, ChallengeGameMode.COMPETITIVE_OR_UNRATED, null, null)
        )).progressTarget()).isEqualByComparingTo(BigDecimal.valueOf(60));
    }

    /**
     * Verifies that a definition is ranked-only as soon as one condition filters on competitive.
     */
    @Test
    void shouldBeCompetitiveOnlyWhenAnyConditionRequiresRanked() {
        ChallengeDefinition mixed = new ChallengeDefinition(3, ProgressMode.ALL, List.of(
            condition(ChallengeMetric.KILLS, 300, ChallengeGameMode.DEATHMATCH, null, null),
            condition(ChallengeMetric.KILLS, 90, ChallengeGameMode.COMPETITIVE, null, null)
        ));
        ChallengeDefinition open = new ChallengeDefinition(3, ProgressMode.SUM, List.of(
            condition(ChallengeMetric.KILLS, 90, ChallengeGameMode.COMPETITIVE_OR_UNRATED, null, null)
        ));

        assertThat(mixed.isCompetitiveOnly()).isTrue();
        assertThat(open.isCompetitiveOnly()).isFalse();
    }

    /**
     * Creates one condition.
     *
     * @param metric      measured statistic
     * @param target      target
     * @param gameMode    game-mode filter
     * @param occurrences occurrences, or {@code null}
     * @param streak      streak, or {@code null}
     * @return condition fixture
     */
    private ChallengeCondition condition(
        ChallengeMetric metric,
        int target,
        ChallengeGameMode gameMode,
        Integer occurrences,
        Integer streak
    ) {
        return new ChallengeCondition(
            metric,
            ChallengeOperator.GTE,
            BigDecimal.valueOf(target),
            gameMode,
            null,
            occurrences != null || streak != null ? ChallengeScope.PER_MATCH : null,
            occurrences,
            streak,
            null
        );
    }
}
