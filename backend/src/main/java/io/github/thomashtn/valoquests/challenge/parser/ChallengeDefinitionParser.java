package io.github.thomashtn.valoquests.challenge.parser;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.SquadLevel;
import java.util.List;

/**
 * Converts persisted challenge JSON rules into typed definitions, and back.
 */
public interface ChallengeDefinitionParser {

    /**
     * Parses and validates one catalogue challenge at the reference level.
     *
     * @param challenge challenge to parse
     * @return typed challenge definition
     */
    ChallengeDefinition parse(Challenge challenge);

    /**
     * Parses and validates one catalogue challenge at the level a campaign plays.
     *
     * @param challenge challenge to parse
     * @param level     squad level whose grid is read
     * @return typed challenge definition
     */
    ChallengeDefinition parse(Challenge challenge, SquadLevel level);

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
