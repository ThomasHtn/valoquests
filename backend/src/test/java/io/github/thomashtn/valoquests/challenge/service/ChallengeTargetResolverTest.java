package io.github.thomashtn.valoquests.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ChallengeGameMode;
import io.github.thomashtn.valoquests.challenge.model.ChallengeGroupBy;
import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.challenge.model.ChallengeOperator;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScaling;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScope;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.model.SkillAnchor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link ChallengeTargetResolver}: the six families of fields and the rounding.
 */
class ChallengeTargetResolverTest {

    /**
     * Amateur squad: a 2 500 reference, 0.47 of the catalogue's, and weaker anchors.
     */
    private static final ChallengeScaling AMATEUR = new ChallengeScaling(
        new BigDecimal("0.47"),
        Map.of(
            SkillAnchor.LONG_KILLS, BigDecimal.valueOf(12),
            SkillAnchor.LONG_KD, new BigDecimal("0.85"),
            SkillAnchor.LONG_ADR, BigDecimal.valueOf(118),
            SkillAnchor.LONG_SCORE, BigDecimal.valueOf(3_600),
            SkillAnchor.DEATHMATCH_KILLS, BigDecimal.valueOf(22)
        )
    );

    /**
     * Elite squad at the volume bound.
     */
    private static final ChallengeScaling ELITE = new ChallengeScaling(BigDecimal.valueOf(3), Map.of());

    /**
     * Resolver under test.
     */
    private final ChallengeTargetResolver resolver = new ChallengeTargetResolver();

    /**
     * Verifies the volume family on the document's own example, tier by tier.
     *
     * <p>The confirmed tier lands on 140 rather than the document's 135: 135.6 rounds to the step
     * of ten the document itself prescribes below one thousand.
     *
     * @param factor   volume factor
     * @param expected expected resolved target for a base of 60 kills
     */
    @ParameterizedTest
    @CsvSource({"0.47, 30", "1.00, 60", "2.26, 140", "3.00, 180"})
    void shouldScaleCumulatedTargetsOnVolume(String factor, int expected) {
        ChallengeScaling scaling = new ChallengeScaling(new BigDecimal(factor), Map.of());
        ChallengeDefinition resolved = resolver.resolve(
            sum(ChallengeMetric.KILLS, 60, ChallengeGameMode.COMPETITIVE_OR_UNRATED),
            ChallengeCadence.WEEKLY,
            ChallengeDifficulty.NORMAL,
            scaling
        );

        assertThat(resolved.singleCondition().target()).isEqualByComparingTo(BigDecimal.valueOf(expected));
    }

    /**
     * Verifies the rounding ladder: readable steps that never fall to zero.
     *
     * @param base     base target
     * @param factor   volume factor
     * @param expected rounded target
     */
    @ParameterizedTest
    @CsvSource({
        "3, 0.47, 1",
        "10, 0.47, 5",
        "24, 0.47, 11",
        "55, 0.47, 25",
        "140, 0.47, 65",
        "4500, 0.47, 2100",
        "12000, 0.47, 5600",
        "27000, 3.00, 81000",
        "12000, 2.26, 27000",
    })
    void shouldRoundVolumeTargetsToAReadableStep(int base, String factor, int expected) {
        ChallengeScaling scaling = new ChallengeScaling(new BigDecimal(factor), Map.of());
        ChallengeDefinition resolved = resolver.resolve(
            sum(ChallengeMetric.KILLS, base, ChallengeGameMode.ANY),
            ChallengeCadence.WEEKLY,
            ChallengeDifficulty.EASY,
            scaling
        );

        assertThat(resolved.singleCondition().target()).isEqualByComparingTo(BigDecimal.valueOf(expected));
    }

    /**
     * Verifies that a per-match bar follows the squad's anchor and the tier's coefficient, and that
     * its occurrences follow the volume.
     */
    @Test
    void shouldScalePerMatchBarsOnTalentAndOccurrencesOnVolume() {
        ChallengeDefinition resolved = resolver.resolve(
            countMatches(ChallengeMetric.KILLS, 18, 6, ChallengeGameMode.COMPETITIVE_OR_UNRATED),
            ChallengeCadence.WEEKLY,
            ChallengeDifficulty.HARD,
            AMATEUR
        );
        ChallengeCondition condition = resolved.singleCondition();

        // 12 kills x 1.18 = 14.16 -> 14; 6 occurrences x 0.47 = 2.82 -> 3.
        assertThat(condition.target()).isEqualByComparingTo(BigDecimal.valueOf(14));
        assertThat(condition.occurrences()).isEqualTo(3);
        assertThat(condition.scope()).isEqualTo(ChallengeScope.PER_MATCH);
        assertThat(condition.gameMode()).isEqualTo(ChallengeGameMode.COMPETITIVE_OR_UNRATED);
    }

    /**
     * Verifies that a ratio keeps two decimals while a per-round average rounds to a step.
     */
    @Test
    void shouldKeepDecimalsOnRates() {
        ChallengeDefinition kd = resolver.resolve(
            countMatches(ChallengeMetric.KD, 1.2, 6, ChallengeGameMode.COMPETITIVE_OR_UNRATED),
            ChallengeCadence.WEEKLY,
            ChallengeDifficulty.HARD,
            AMATEUR
        );
        ChallengeDefinition adr = resolver.resolve(
            countMatches(ChallengeMetric.ADR, 170, 5, ChallengeGameMode.COMPETITIVE),
            ChallengeCadence.WEEKLY,
            ChallengeDifficulty.VERY_HARD,
            AMATEUR
        );

        // 0.85 x 1.18 = 1.003 -> 1.00; 118 x 1.32 = 155.76 -> 155 (step of five above one hundred).
        assertThat(kd.singleCondition().target()).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(adr.singleCondition().target()).isEqualByComparingTo(BigDecimal.valueOf(155));
    }

    /**
     * Verifies the coefficients of every tier on one anchor.
     *
     * @param difficulty tier
     * @param expected   resolved per-match kills for an anchor of 12
     */
    @ParameterizedTest
    @CsvSource({"EASY, 11", "NORMAL, 12", "MEDIUM, 13", "HARD, 14", "VERY_HARD, 16"})
    void shouldApplyTheTierCoefficientToTalentAnchors(ChallengeDifficulty difficulty, int expected) {
        ChallengeDefinition resolved = resolver.resolve(
            countMatches(ChallengeMetric.KILLS, 15, 4, ChallengeGameMode.COMPETITIVE),
            ChallengeCadence.WEEKLY,
            difficulty,
            AMATEUR
        );

        assertThat(resolved.singleCondition().target()).isEqualByComparingTo(BigDecimal.valueOf(expected));
    }

    /**
     * Verifies that a daily bar uses its own coefficient and keeps its occurrences.
     */
    @Test
    void shouldResolveADailyBarWithTheDailyCoefficientAndFixedOccurrences() {
        ChallengeDefinition resolved = resolver.resolve(
            countMatches(ChallengeMetric.KILLS, 20, 2, ChallengeGameMode.DEATHMATCH),
            ChallengeCadence.DAILY,
            null,
            AMATEUR
        );
        ChallengeCondition condition = resolved.singleCondition();

        // 22 x 0.85 = 18.7 -> 19; two matches stay two matches.
        assertThat(condition.target()).isEqualByComparingTo(BigDecimal.valueOf(19));
        assertThat(condition.occurrences()).isEqualTo(2);
    }

    /**
     * Verifies that a bar whose anchor the squad does not measure keeps its base value.
     */
    @Test
    void shouldKeepTheBaseBarWhenTheSquadHasNoAnchor() {
        ChallengeDefinition resolved = resolver.resolve(
            countMatches(ChallengeMetric.KILLS, 35, 5, ChallengeGameMode.TEAM_DEATHMATCH),
            ChallengeCadence.WEEKLY,
            ChallengeDifficulty.HARD,
            AMATEUR
        );

        assertThat(resolved.singleCondition().target()).isEqualByComparingTo(BigDecimal.valueOf(35));
        assertThat(resolved.singleCondition().occurrences()).isEqualTo(2);
    }

    /**
     * Verifies that days, agents and modes are never scaled, on either cadence.
     */
    @Test
    void shouldNeverScaleDaysAgentsOrModes() {
        ProgressMode distinct = ProgressMode.DISTINCT_COUNT;
        ProgressMode best = ProgressMode.MAX_GROUP;

        assertFixed(grouped(distinct, ChallengeMetric.PLAY_DAY, 4, ChallengeGroupBy.PLAY_DAY), 4);
        assertFixed(grouped(distinct, ChallengeMetric.MATCHES_PLAYED, 3, ChallengeGroupBy.AGENT), 3);
        assertFixed(grouped(distinct, ChallengeMetric.MATCHES_PLAYED, 5, ChallengeGroupBy.GAME_MODE), 5);
        assertFixed(grouped(distinct, ChallengeMetric.MATCHES_WON, 3, ChallengeGroupBy.PLAY_DAY), 3);
        assertFixed(grouped(best, ChallengeMetric.MATCHES_WON, 2, ChallengeGroupBy.PLAY_DAY), 2);
        assertFixed(grouped(best, ChallengeMetric.MATCHES_PLAYED, 3, ChallengeGroupBy.PLAY_DAY), 3);
    }

    /**
     * Verifies that a daily match count is fixed while a daily kill count follows the volume.
     */
    @Test
    void shouldFixDailyMatchCountsAndScaleDailyKillCounts() {
        ChallengeDefinition matches = resolver.resolve(
            sum(ChallengeMetric.MATCHES_PLAYED, 2, ChallengeGameMode.ANY),
            ChallengeCadence.DAILY,
            null,
            ELITE
        );
        ChallengeDefinition kills = resolver.resolve(
            sum(ChallengeMetric.KILLS, 30, ChallengeGameMode.ANY),
            ChallengeCadence.DAILY,
            null,
            ELITE
        );

        assertThat(matches.singleCondition().target()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(kills.singleCondition().target()).isEqualByComparingTo(BigDecimal.valueOf(90));
    }

    /**
     * Verifies that the best day of the week scales its kills but not its matches.
     */
    @Test
    void shouldScaleABestDayKillCountOnVolume() {
        ChallengeDefinition resolved = resolver.resolve(
            grouped(ProgressMode.MAX_GROUP, ChallengeMetric.KILLS, 30, ChallengeGroupBy.PLAY_DAY),
            ChallengeCadence.WEEKLY,
            ChallengeDifficulty.EASY,
            ELITE
        );

        assertThat(resolved.singleCondition().target()).isEqualByComparingTo(BigDecimal.valueOf(90));
    }

    /**
     * Verifies that a composite challenge scales each of its conditions.
     */
    @Test
    void shouldScaleEveryConditionOfAComposite() {
        ChallengeDefinition resolved = resolver.resolve(
            new ChallengeDefinition(3, ProgressMode.ALL, List.of(
                condition(ChallengeMetric.MATCHES_PLAYED, 10, ChallengeGameMode.DEATHMATCH),
                condition(ChallengeMetric.MATCHES_PLAYED, 6, ChallengeGameMode.TEAM_DEATHMATCH)
            )),
            ChallengeCadence.WEEKLY,
            ChallengeDifficulty.HARD,
            AMATEUR
        );

        assertThat(resolved.conditions())
            .extracting(ChallengeCondition::target)
            .usingElementComparator(BigDecimal::compareTo)
            .containsExactly(BigDecimal.valueOf(5), BigDecimal.valueOf(3));
    }

    /**
     * Verifies that the neutral scaling hands the base definition back untouched.
     */
    @Test
    void shouldReturnTheBaseDefinitionOutsideAnyCampaign() {
        ChallengeDefinition base = countMatches(ChallengeMetric.KD, 1.2, 6, ChallengeGameMode.COMPETITIVE);

        assertThat(resolver.resolve(base, ChallengeCadence.WEEKLY, ChallengeDifficulty.HARD, ChallengeScaling.NONE))
            .isSameAs(base);
    }

    /**
     * Asserts that a definition resolves to the same target at both bounds of the volume factor.
     *
     * @param definition definition under test
     * @param expected   fixed target
     */
    private void assertFixed(ChallengeDefinition definition, int expected) {
        for (ChallengeScaling scaling : List.of(AMATEUR, ELITE)) {
            ChallengeDefinition resolved =
                resolver.resolve(definition, ChallengeCadence.WEEKLY, ChallengeDifficulty.MEDIUM, scaling);

            assertThat(resolved.singleCondition().target())
                .as("%s stays fixed", definition.singleCondition().metric())
                .isEqualByComparingTo(BigDecimal.valueOf(expected));
        }
    }

    /**
     * Creates a summed definition.
     *
     * @param metric   metric
     * @param target   base target
     * @param gameMode game-mode filter
     * @return definition
     */
    private ChallengeDefinition sum(ChallengeMetric metric, int target, ChallengeGameMode gameMode) {
        return new ChallengeDefinition(3, ProgressMode.SUM, List.of(condition(metric, target, gameMode)));
    }

    /**
     * Creates a count-matches definition.
     *
     * @param metric      metric
     * @param target      per-match bar
     * @param occurrences matches that must clear it
     * @param gameMode    game-mode filter
     * @return definition
     */
    private ChallengeDefinition countMatches(
        ChallengeMetric metric,
        double target,
        int occurrences,
        ChallengeGameMode gameMode
    ) {
        return new ChallengeDefinition(3, ProgressMode.COUNT_MATCHES, List.of(new ChallengeCondition(
            metric,
            ChallengeOperator.GTE,
            BigDecimal.valueOf(target),
            gameMode,
            null,
            ChallengeScope.PER_MATCH,
            occurrences,
            null,
            null
        )));
    }

    /**
     * Creates a grouped definition without a game-mode filter.
     *
     * @param progressMode grouped progress mode
     * @param metric       metric
     * @param target       base target
     * @param groupBy      grouping dimension
     * @return definition
     */
    private ChallengeDefinition grouped(
        ProgressMode progressMode,
        ChallengeMetric metric,
        int target,
        ChallengeGroupBy groupBy
    ) {
        return new ChallengeDefinition(3, progressMode, List.of(new ChallengeCondition(
            metric,
            ChallengeOperator.GTE,
            BigDecimal.valueOf(target),
            ChallengeGameMode.ANY,
            groupBy,
            null,
            null,
            null,
            null
        )));
    }

    /**
     * Creates one summed condition.
     *
     * @param metric   metric
     * @param target   base target
     * @param gameMode game-mode filter
     * @return condition
     */
    private ChallengeCondition condition(ChallengeMetric metric, int target, ChallengeGameMode gameMode) {
        return new ChallengeCondition(
            metric,
            ChallengeOperator.GTE,
            BigDecimal.valueOf(target),
            gameMode,
            null,
            null,
            null,
            null,
            null
        );
    }
}
