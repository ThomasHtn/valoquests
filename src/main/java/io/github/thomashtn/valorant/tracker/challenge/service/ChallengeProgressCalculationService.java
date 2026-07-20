package io.github.thomashtn.valorant.tracker.challenge.service;

import io.github.thomashtn.valorant.tracker.challenge.calculator.ChallengeProgressCalculator;
import io.github.thomashtn.valorant.tracker.challenge.calculator.ChallengeProgressCalculatorRegistry;
import io.github.thomashtn.valorant.tracker.challenge.calculator.ChallengeProgressResult;
import io.github.thomashtn.valorant.tracker.challenge.calculator.PlayerChallengeContext;
import io.github.thomashtn.valorant.tracker.challenge.entity.Challenge;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valorant.tracker.challenge.parser.ChallengeDefinitionParser;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Coordinates the parsing and calculation of one challenge for one player.
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
     * Calculates the progress of one player for one challenge.
     *
     * @param challenge persisted challenge
     * @param context   player challenge calculation context
     * @return calculated challenge progress
     */
    public ChallengeProgressResult calculate(
        Challenge challenge,
        PlayerChallengeContext context
    ) {
        Objects.requireNonNull(
            challenge,
            "Challenge must not be null."
        );

        Objects.requireNonNull(
            context,
            "Player challenge context must not be null."
        );

        ChallengeDefinition definition =
            definitionParser.parse(challenge);

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
