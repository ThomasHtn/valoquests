package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressCalculator;
import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressCalculatorRegistry;
import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressResult;
import io.github.thomashtn.valoquests.challenge.calculator.PlayerChallengeContext;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Coordinates the parsing and calculation of one selected challenge for one player.
 *
 * <p>Always evaluates the definition resolved at draw time, never the catalogue's base one.
 */
@Service
public class ChallengeProgressCalculationService {

    /**
     * Parser used to convert persisted JSON rules into typed definitions.
     */
    private final ChallengeDefinitionParser definitionParser;

    /**
     * Registry used to select the appropriate progress calculator.
     */
    private final ChallengeProgressCalculatorRegistry calculatorRegistry;

    /**
     * Creates the challenge calculation service.
     *
     * @param definitionParser   persisted challenge-definition parser
     * @param calculatorRegistry progress calculator registry
     */
    public ChallengeProgressCalculationService(
        ChallengeDefinitionParser definitionParser,
        ChallengeProgressCalculatorRegistry calculatorRegistry
    ) {
        this.definitionParser = definitionParser;
        this.calculatorRegistry = calculatorRegistry;
    }

    /**
     * Calculates the progress of one player for one selected challenge.
     *
     * @param selection weekly or daily selection, with its resolved conditions
     * @param context   player challenge calculation context over the selection's period
     * @return calculated challenge progress
     */
    public ChallengeProgressResult calculate(
        WeeklyChallenge selection,
        PlayerChallengeContext context
    ) {
        Objects.requireNonNull(
            selection,
            "Selection must not be null."
        );

        Objects.requireNonNull(
            context,
            "Player challenge context must not be null."
        );

        ChallengeDefinition definition =
            definitionParser.parse(selection);

        ChallengeProgressCalculator calculator =
            calculatorRegistry.getCalculator(
                definition.progressMode()
            );

        return calculator.calculate(
            definition,
            context
        );
    }
}
