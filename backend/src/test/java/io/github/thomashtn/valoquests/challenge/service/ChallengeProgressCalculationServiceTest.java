package io.github.thomashtn.valoquests.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressCalculator;
import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressCalculatorRegistry;
import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressResult;
import io.github.thomashtn.valoquests.challenge.calculator.PlayerChallengeContext;
import io.github.thomashtn.valoquests.challenge.calculator.PlayerChallengeContextFactory;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.model.CalculatedProgress;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests challenge progress calculation orchestration.
 */
class ChallengeProgressCalculationServiceTest {

    /**
     * Challenge definition parser dependency.
     */
    private ChallengeDefinitionParser definitionParser;

    /**
     * Calculator registry dependency.
     */
    private ChallengeProgressCalculatorRegistry calculatorRegistry;

    /**
     * Calculator selected by the registry.
     */
    private ChallengeProgressCalculator calculator;

    /**
     * Player context factory dependency.
     */
    private PlayerChallengeContextFactory contextFactory;

    /**
     * Service under test.
     */
    private ChallengeProgressCalculationService service;

    /**
     * Creates test dependencies before each test.
     */
    @BeforeEach
    void setUp() {
        definitionParser = mock(ChallengeDefinitionParser.class);
        calculatorRegistry =
            mock(ChallengeProgressCalculatorRegistry.class);
        calculator = mock(ChallengeProgressCalculator.class);
        contextFactory = mock(PlayerChallengeContextFactory.class);

        service = new ChallengeProgressCalculationService(
            definitionParser,
            calculatorRegistry,
            contextFactory,
            new WeekCalendar(
                Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC),
                ZoneOffset.UTC
            )
        );
    }

    /**
     * Verifies that a week's selections share one load of its matches, a weekly selection being
     * evaluated over the whole week and a daily one over its own day only.
     */
    @Test
    @DisplayName("Evaluates a weekly selection over the whole week and a daily one over its own day")
    void shouldEvaluateAWeeklySelectionOverTheWeekAndADailyOneOverItsDay() {
        Player player = new Player();
        player.setDisplayName("Psilonnix");
        LocalDate weekStart = LocalDate.of(2026, 7, 20);
        PlayerMatch yesterday = matchAt(Instant.parse("2026-07-21T23:30:00Z"));
        PlayerMatch today = matchAt(Instant.parse("2026-07-22T09:00:00Z"));
        PlayerChallengeContext weekContext = new PlayerChallengeContext(List.of(yesterday, today));
        ChallengeSelection weekly = selection(ChallengeCadence.WEEKLY, null);
        ChallengeSelection daily = selection(ChallengeCadence.DAILY, LocalDate.of(2026, 7, 22));
        ChallengeDefinition definition = mock(ChallengeDefinition.class);
        ChallengeProgressResult result = ChallengeProgressResult.from(BigDecimal.ONE, BigDecimal.TEN);

        when(contextFactory.create(player, weekStart)).thenReturn(weekContext);
        when(definitionParser.parse(any(ChallengeSelection.class))).thenReturn(definition);
        when(definition.progressMode()).thenReturn(ProgressMode.SUM);
        when(calculatorRegistry.getCalculator(ProgressMode.SUM)).thenReturn(calculator);
        when(calculator.calculate(eq(definition), any())).thenReturn(result);

        List<CalculatedProgress> calculated = service.calculateWeek(player, weekStart, List.of(weekly, daily));

        ArgumentCaptor<PlayerChallengeContext> contexts = ArgumentCaptor.forClass(PlayerChallengeContext.class);
        verify(calculator, times(2)).calculate(eq(definition), contexts.capture());

        assertThat(calculated).containsExactly(
            new CalculatedProgress(weekly, result),
            new CalculatedProgress(daily, result)
        );
        assertThat(contexts.getAllValues().get(0)).isSameAs(weekContext);
        assertThat(contexts.getAllValues().get(1).playerMatches()).containsExactly(today);
    }

    /**
     * Verifies that the service parses the selection's resolved definition, selects the calculator
     * and returns its calculation result.
     */
    @Test
    void shouldCalculateChallengeProgress() {
        ChallengeSelection challenge = mock(ChallengeSelection.class);
        PlayerChallengeContext context =
            mock(PlayerChallengeContext.class);
        ChallengeDefinition definition =
            mock(ChallengeDefinition.class);

        ChallengeProgressResult expectedResult =
            ChallengeProgressResult.from(
                BigDecimal.valueOf(42),
                BigDecimal.valueOf(100)
            );

        when(definitionParser.parse(challenge))
            .thenReturn(definition);

        when(definition.progressMode())
            .thenReturn(ProgressMode.SUM);

        when(calculatorRegistry.getCalculator(ProgressMode.SUM))
            .thenReturn(calculator);

        when(calculator.calculate(definition, context))
            .thenReturn(expectedResult);

        ChallengeProgressResult result =
            service.calculate(
                challenge,
                context
            );

        assertThat(result).isSameAs(expectedResult);

        verify(definitionParser).parse(challenge);
        verify(calculatorRegistry).getCalculator(ProgressMode.SUM);
        verify(calculator).calculate(definition, context);
    }

    /**
     * Creates a selection of one cadence.
     *
     * @param cadence weekly or daily
     * @param day     covered day, {@code null} for a weekly selection
     * @return selection fixture
     */
    private ChallengeSelection selection(ChallengeCadence cadence, LocalDate day) {
        Challenge challenge = new Challenge();
        challenge.setCode(cadence + "_CHALLENGE");

        ChallengeSelection selection = new ChallengeSelection();
        selection.setCadence(cadence);
        selection.setDay(day);
        selection.setChallenge(challenge);
        return selection;
    }

    /**
     * Creates a player match started at one instant.
     *
     * @param startedAt match start
     * @return player match fixture
     */
    private PlayerMatch matchAt(Instant startedAt) {
        ValorantMatch match = new ValorantMatch();
        match.setStartedAt(startedAt);

        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setMatch(match);
        return playerMatch;
    }
}
