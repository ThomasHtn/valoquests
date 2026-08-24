package io.github.thomashtn.valoquests.challenge.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeGameMode;
import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.challenge.model.ChallengeOperator;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.match.service.MatchEligibility;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies that progression challenges measure a player against their own recent form, and that they
 * cannot be satisfied by volume, by absence, or by having no past at all.
 */
class BaselineChallengeProgressCalculatorTest {

    /** Week being evaluated. */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 20);

    /** Inclusive start of that week. */
    private static final Instant PERIOD_START = Instant.parse("2026-07-20T00:00:00Z");

    /** Exclusive end of that week. */
    private static final Instant PERIOD_END = Instant.parse("2026-07-27T00:00:00Z");

    /** Increments the identifier and timestamp of each fixture match. */
    private long matchSequence;

    /** Calculator under test, wired like Spring wires it. */
    private final BaselineChallengeProgressCalculator calculator =
        new BaselineChallengeProgressCalculator(
            new AggregateRateCalculator(),
            new ChallengeMatchFilter(new MatchEligibility())
        );

    /**
     * Verifies that a player beating their baseline by more than the target completes the challenge.
     */
    @Test
    void shouldCompleteWhenTheWeekBeatsTheBaselineByTheRequiredMargin() {
        // Baseline K/D of 1.0 over four matches, week K/D of 1.5 over four: a 50% gain.
        ChallengeProgressResult result = calculator.calculate(
            definition(ChallengeMetric.KD, 10, 4),
            context(
                List.of(match(15, 10), match(15, 10), match(15, 10), match(15, 10)),
                List.of(match(10, 10), match(10, 10), match(10, 10), match(10, 10))
            )
        );

        assertThat(result.completed()).isTrue();
        assertThat(result.currentValue()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(result.targetValue()).isEqualByComparingTo(BigDecimal.TEN);
    }

    /**
     * Verifies that matching the baseline exactly is not an improvement.
     */
    @Test
    void shouldNotCompleteWhenTheWeekOnlyMatchesTheBaseline() {
        ChallengeProgressResult result = calculator.calculate(
            definition(ChallengeMetric.KD, 10, 4),
            context(
                List.of(match(10, 10), match(10, 10), match(10, 10), match(10, 10)),
                List.of(match(10, 10), match(10, 10), match(10, 10), match(10, 10))
            )
        );

        assertThat(result.completed()).isFalse();
        assertThat(result.currentValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * Verifies that a player who regressed reads as zero progress rather than as a negative number.
     */
    @Test
    void shouldFloorProgressAtZeroWhenThePlayerRegressed() {
        ChallengeProgressResult result = calculator.calculate(
            definition(ChallengeMetric.KD, 10, 4),
            context(
                List.of(match(5, 10), match(5, 10), match(5, 10), match(5, 10)),
                List.of(match(10, 10), match(10, 10), match(10, 10), match(10, 10))
            )
        );

        assertThat(result.completed()).isFalse();
        assertThat(result.currentValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.progressPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * The property this whole mode exists for: playing more matches at the same level moves the
     * numerator and the denominator together, so volume alone cannot complete a progression challenge.
     */
    @Test
    void shouldNotBeFarmableByPlayingMoreMatchesAtTheSameLevel() {
        List<PlayerMatch> baseline = List.of(match(10, 10), match(10, 10), match(10, 10), match(10, 10));

        ChallengeProgressResult fewMatches = calculator.calculate(
            definition(ChallengeMetric.KD, 5, 4),
            context(List.of(match(10, 10), match(10, 10), match(10, 10), match(10, 10)), baseline)
        );

        List<PlayerMatch> manyMatches = new java.util.ArrayList<>();
        for (int index = 0; index < 40; index++) {
            manyMatches.add(match(10, 10));
        }

        ChallengeProgressResult manyMatchesResult = calculator.calculate(
            definition(ChallengeMetric.KD, 5, 4),
            context(manyMatches, baseline)
        );

        assertThat(fewMatches.completed()).isFalse();
        assertThat(manyMatchesResult.completed()).isFalse();
        assertThat(manyMatchesResult.currentValue())
            .isEqualByComparingTo(fewMatches.currentValue());
    }

    /**
     * Verifies that a player with no baseline cannot complete the challenge.
     *
     * <p>Otherwise a month of absence would be rewarded with a free week, which is the opposite of what
     * the roster's shared responsibility is supposed to mean.
     */
    @Test
    void shouldNotCompleteWithoutABaseline() {
        ChallengeProgressResult result = calculator.calculate(
            definition(ChallengeMetric.KD, 5, 4),
            context(
                List.of(match(30, 1), match(30, 1), match(30, 1), match(30, 1)),
                List.of()
            )
        );

        assertThat(result.completed()).isFalse();
        assertThat(result.currentValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * Verifies that a strong but tiny sample does not complete the challenge.
     */
    @Test
    void shouldNotCompleteBelowTheMinimumNumberOfMatches() {
        ChallengeProgressResult result = calculator.calculate(
            definition(ChallengeMetric.KD, 5, 4),
            context(
                List.of(match(40, 1)),
                List.of(match(10, 10), match(10, 10), match(10, 10), match(10, 10))
            )
        );

        assertThat(result.completed()).isFalse();
    }

    /**
     * Verifies that an ineligible match neither feeds the week nor the baseline.
     */
    @Test
    void shouldIgnoreRemakesOnBothSidesOfTheComparison() {
        PlayerMatch remake = match(0, 0);
        remake.setResult(MatchResult.REMAKE);
        remake.setRoundsPlayed(2);
        remake.setScore(0);

        ChallengeProgressResult withRemake = calculator.calculate(
            definition(ChallengeMetric.KD, 10, 4),
            context(
                List.of(match(15, 10), match(15, 10), match(15, 10), match(15, 10), remake),
                List.of(match(10, 10), match(10, 10), match(10, 10), match(10, 10))
            )
        );

        assertThat(withRemake.completed()).isTrue();
        assertThat(withRemake.currentValue()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    /**
     * Verifies that the headshot rate is compared as a share of kills, not as a headshot total.
     */
    @Test
    void shouldCompareHeadshotRateRatherThanHeadshotVolume() {
        PlayerMatch baselineMatch = match(20, 10);
        baselineMatch.setHeadshots(4);

        PlayerMatch weekMatch = match(20, 10);
        weekMatch.setHeadshots(6);

        ChallengeProgressResult result = calculator.calculate(
            definition(ChallengeMetric.HEADSHOT_RATE, 25, 1),
            context(List.of(weekMatch), List.of(baselineMatch))
        );

        // 30% of kills against 20%, so a 50% relative gain, comfortably past the 25% asked for.
        assertThat(result.completed()).isTrue();
        assertThat(result.currentValue()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    /** Builds a single-condition baseline definition. */
    private ChallengeDefinition definition(
        ChallengeMetric metric,
        int requiredGainPercent,
        int minimumMatches
    ) {
        return new ChallengeDefinition(
            3,
            ProgressMode.BASELINE,
            List.of(new ChallengeCondition(
                metric,
                ChallengeOperator.GTE,
                BigDecimal.valueOf(requiredGainPercent),
                ChallengeGameMode.COMPETITIVE,
                null,
                null,
                null,
                null,
                minimumMatches
            ))
        );
    }

    /** Builds a context carrying a week and its baseline window. */
    private PlayerChallengeContext context(
        List<PlayerMatch> weekMatches,
        List<PlayerMatch> baselineMatches
    ) {
        return new PlayerChallengeContext(
            1L,
            WEEK_START,
            PERIOD_START,
            PERIOD_END,
            weekMatches,
            baselineMatches
        );
    }

    /** Builds an eligible competitive match fixture. */
    private PlayerMatch match(int kills, int deaths) {
        matchSequence++;

        ValorantMatch valorantMatch = new ValorantMatch();
        valorantMatch.setGameMode(GameMode.COMPETITIVE);
        valorantMatch.setStartedAt(PERIOD_START.plusSeconds(matchSequence * 3_600));

        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setId(matchSequence);
        playerMatch.setMatch(valorantMatch);
        playerMatch.setResult(MatchResult.WIN);
        playerMatch.setRoundsPlayed(20);
        playerMatch.setScore(5_000);
        playerMatch.setKills(kills);
        playerMatch.setDeaths(deaths);
        playerMatch.setHeadshots(0);
        playerMatch.setDamageDealt(3_000);

        return playerMatch;
    }
}
