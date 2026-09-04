package io.github.thomashtn.valoquests.challenge.parser;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import java.util.List;

/**
 * Converts persisted challenge JSON rules into typed definitions, and back.
 */
public interface ChallengeDefinitionParser {

    /**
     * Parses and validates one catalogue challenge: its base, unscaled definition.
     *
     * @param challenge challenge to parse
     * @return typed challenge definition
     */
    ChallengeDefinition parse(Challenge challenge);

    /**
     * Parses and validates the definition a selection was resolved to at draw time.
     *
     * <p>This is the definition calculators evaluate and the interface displays; the catalogue's
     * own definition is only ever an input to the draw.
     *
     * @param selection weekly or daily selection to parse
     * @return typed resolved definition
     */
    ChallengeDefinition parse(WeeklyChallenge selection);

    /**
     * Serializes resolved conditions in the shape {@link #parse(WeeklyChallenge)} reads back.
     *
     * @param conditions resolved conditions
     * @return JSON array
     */
    String toJson(List<ChallengeCondition> conditions);
}
