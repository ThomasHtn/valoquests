package io.github.thomashtn.valoquests.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressCalculator;
import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressCalculatorRegistry;
import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressResult;
import io.github.thomashtn.valoquests.challenge.calculator.PlayerChallengeContext;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

        service = new ChallengeProgressCalculationService(
            definitionParser,
            calculatorRegistry
        );
    }

    /**
     * Verifies that the service parses the challenge, selects the calculator
     * and returns its calculation result.
     */
    @Test
    void shouldCalculateChallengeProgress() {
        Challenge challenge = mock(Challenge.class);
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
}
