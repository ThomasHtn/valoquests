package io.github.thomashtn.valorant.tracker.challenge.parser;

import io.github.thomashtn.valorant.tracker.challenge.entity.Challenge;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDefinition;

/**
 * Converts persisted challenge JSON rules into typed definitions.
 */
public interface ChallengeDefinitionParser {

    /**
     * Parses and validates one persisted challenge.
     *
     * @param challenge challenge to parse
     * @return typed challenge definition
     */
    ChallengeDefinition parse(Challenge challenge);
}
