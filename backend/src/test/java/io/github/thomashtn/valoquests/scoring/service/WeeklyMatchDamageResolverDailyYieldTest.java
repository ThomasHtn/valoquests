package io.github.thomashtn.valoquests.scoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies where {@link WeeklyMatchDamageResolver#dailyYield} places a player on the day's ladder.
 *
 * <p>The ladder itself belongs to the ruleset and is tested there; what matters here is that the
 * standing reported is the one the *next* match will meet, and that the step below it is found by
 * probing rather than by a second copy of the thresholds.
 */
@ExtendWith(MockitoExtension.class)
class WeeklyMatchDamageResolverDailyYieldTest {

    /**
     * The day every match in these tests falls on.
     */
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 31);

    /**
     * Mocked eligibility and pricing of a single match.
     */
    @Mock
    private MatchDamageCalculator matchDamageCalculator;

    /**
     * Mocked calendar resolving the day a match falls on.
     */
    @Mock
    private WeekCalendar weekCalendar;

    /**
     * Mocked ruleset supplying the coefficient ladder.
     */
    @Mock
    private ScoringRuleset ruleset;

    /**
     * Resolver under test.
     */
    private WeeklyMatchDamageResolver resolver;

    /**
     * Creates the resolver and the standard three-band ladder before each test.
     */
    @BeforeEach
    void setUp() {
        resolver = new WeeklyMatchDamageResolver(matchDamageCalculator, weekCalendar);

        lenient().when(matchDamageCalculator.isEligible(any())).thenReturn(true);
        lenient().when(weekCalendar.dayOf(any())).thenReturn(TODAY);
        lenient().when(ruleset.matchDamageCoefficientPercent(org.mockito.ArgumentMatchers.anyInt()))
            .thenAnswer(invocation -> {
                int rank = invocation.getArgument(0);
                if (rank <= 5) {
                    return 100;
                }
                return rank <= 9 ? 50 : 25;
            });
    }

    /**
     * Verifies that a player who has not played yet is told their first match counts in full, and
     * where the ladder first steps down.
     */
    @Test
    void shouldReportTheFullBandAndItsFirstStepDownOnAnUntouchedDay() {
        WeeklyMatchDamageResolver.DailyYield yield =
            resolver.dailyYield(List.of(), ruleset, TODAY);

        assertThat(yield.matchesToday()).isZero();
        assertThat(yield.nextMatchPercent()).isEqualTo(100);
        assertThat(yield.dropsAtRank()).isEqualTo(6);
        assertThat(yield.dropsToPercent()).isEqualTo(50);
    }

    /**
     * Verifies that the standing is read for the match about to be played, not the last one played:
     * after four matches the fifth still counts in full, and the drop is still the sixth.
     */
    @Test
    void shouldReportTheStandingOfTheNextMatchRatherThanTheLastOne() {
        WeeklyMatchDamageResolver.DailyYield yield =
            resolver.dailyYield(matchesPlayed(4), ruleset, TODAY);

        assertThat(yield.matchesToday()).isEqualTo(4);
        assertThat(yield.nextMatchPercent()).isEqualTo(100);
        assertThat(yield.dropsAtRank()).isEqualTo(6);
    }

    /**
     * Verifies that a player already inside the reduced band is told so, and pointed at the next
     * step rather than at the one they have already crossed.
     */
    @Test
    void shouldReportTheNextStepDownFromInsideAReducedBand() {
        WeeklyMatchDamageResolver.DailyYield yield =
            resolver.dailyYield(matchesPlayed(6), ruleset, TODAY);

        assertThat(yield.nextMatchPercent()).isEqualTo(50);
        assertThat(yield.dropsAtRank()).isEqualTo(10);
        assertThat(yield.dropsToPercent()).isEqualTo(25);
    }

    /**
     * Verifies that the ladder's floor reports no further step, rather than an invented one.
     */
    @Test
    void shouldReportNoFurtherStepOnceTheLadderHasBottomedOut() {
        WeeklyMatchDamageResolver.DailyYield yield =
            resolver.dailyYield(matchesPlayed(12), ruleset, TODAY);

        assertThat(yield.nextMatchPercent()).isEqualTo(25);
        assertThat(yield.dropsAtRank()).isNull();
        assertThat(yield.dropsToPercent()).isNull();
    }

    /**
     * Verifies that matches of another day are not counted, whatever the list holds.
     */
    @Test
    void shouldCountOnlyTheMatchesOfTheDayAskedAbout() {
        when(weekCalendar.dayOf(any())).thenReturn(TODAY.minusDays(1));

        WeeklyMatchDamageResolver.DailyYield yield =
            resolver.dailyYield(matchesPlayed(7), ruleset, TODAY);

        assertThat(yield.matchesToday()).isZero();
        assertThat(yield.nextMatchPercent()).isEqualTo(100);
    }

    /**
     * Verifies that matches the ruleset does not value never enter the ladder.
     */
    @Test
    void shouldIgnoreMatchesTheRulesetDoesNotValue() {
        when(matchDamageCalculator.isEligible(any())).thenReturn(false);

        WeeklyMatchDamageResolver.DailyYield yield =
            resolver.dailyYield(matchesPlayed(7), ruleset, TODAY);

        assertThat(yield.matchesToday()).isZero();
    }

    /**
     * Builds a number of player matches, each carrying the minimum a resolver reads from one.
     *
     * @param count how many matches to build
     * @return the matches
     */
    private List<PlayerMatch> matchesPlayed(int count) {
        return java.util.stream.IntStream.range(0, count).mapToObj(index -> {
            ValorantMatch match = new ValorantMatch();
            match.setStartedAt(Instant.parse("2026-08-31T10:00:00Z").plusSeconds(index * 60L));

            PlayerMatch playerMatch = new PlayerMatch();
            playerMatch.setMatch(match);
            return playerMatch;
        }).toList();
    }
}
