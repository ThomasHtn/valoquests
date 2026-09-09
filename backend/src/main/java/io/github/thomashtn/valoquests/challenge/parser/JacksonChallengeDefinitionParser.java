package io.github.thomashtn.valoquests.challenge.parser;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Jackson-based implementation of the challenge-definition parser.
 */
@Component
public class JacksonChallengeDefinitionParser
    implements ChallengeDefinitionParser {

    /**
     * Current rule-schema version supported by the application.
     */
    private static final int SUPPORTED_SCHEMA_VERSION = 3;

    /**
     * Jackson type token used to deserialize the JSON condition array.
     */
    private static final TypeReference<List<ChallengeCondition>> CONDITION_LIST_TYPE =
        new TypeReference<>() {
        };

    /**
     * Application-configured JSON mapper.
     */
    private final ObjectMapper objectMapper;

    /**
     * Creates the challenge-definition parser.
     *
     * @param objectMapper application JSON mapper
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public JacksonChallengeDefinitionParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses and validates one persisted challenge definition.
     *
     * @param challenge challenge to parse
     * @return typed challenge definition
     */
    @Override
    public ChallengeDefinition parse(Challenge challenge) {
        Objects.requireNonNull(
            challenge,
            "Challenge must not be null."
        );

        return parse(challenge, challenge.getConditionsJson());
    }

    /**
     * Parses and validates the resolved definition stored on one selection.
     *
     * @param selection selection to parse
     * @return typed resolved definition
     */
    @Override
    public ChallengeDefinition parse(WeeklyChallenge selection) {
        Objects.requireNonNull(selection, "Selection must not be null.");

        return parse(selection.getChallenge(), selection.getResolvedConditionsJson());
    }

    /**
     * Serializes resolved conditions.
     *
     * @param conditions resolved conditions
     * @return JSON array
     */
    @Override
    public String toJson(List<ChallengeCondition> conditions) {
        Objects.requireNonNull(conditions, "Conditions must not be null.");

        return objectMapper.writeValueAsString(conditions);
    }

    /**
     * Parses one JSON rule against the challenge that owns it.
     *
     * @param challenge      challenge providing the schema version and progress mode
     * @param conditionsJson JSON array to parse, base or resolved
     * @return typed challenge definition
     */
    private ChallengeDefinition parse(Challenge challenge, String conditionsJson) {
        validateChallengeMetadata(challenge, conditionsJson);

        List<ChallengeCondition> conditions = parseConditions(challenge, conditionsJson);

        ChallengeDefinition definition = new ChallengeDefinition(
            challenge.getSchemaVersion(),
            challenge.getProgressMode(),
            conditions
        );

        ChallengeDefinitionValidator.validate(challenge, definition);

        return definition;
    }

    /**
     * Deserializes one JSON condition array.
     *
     * @param challenge      challenge owning the rule, for error messages
     * @param conditionsJson JSON array to parse
     * @return parsed conditions
     */
    private List<ChallengeCondition> parseConditions(Challenge challenge, String conditionsJson) {
        try {
            return objectMapper.readValue(
                conditionsJson,
                CONDITION_LIST_TYPE
            );
        } catch (JacksonException exception) {
            throw ChallengeDefinitionValidator.invalidDefinition(
                challenge,
                "The conditions JSON cannot be parsed.",
                exception
            );
        }
    }

    /**
     * Validates fields stored outside the JSON condition document.
     *
     * @param challenge      challenge being validated
     * @param conditionsJson JSON array about to be parsed
     */
    private void validateChallengeMetadata(Challenge challenge, String conditionsJson) {
        if (challenge.getCode() == null || challenge.getCode().isBlank()) {
            throw ChallengeDefinitionValidator.invalidDefinition(
                challenge,
                "The challenge code must not be blank."
            );
        }

        if (challenge.getSchemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            throw ChallengeDefinitionValidator.invalidDefinition(
                challenge,
                "Unsupported schema version "
                    + challenge.getSchemaVersion()
                    + ". Expected "
                    + SUPPORTED_SCHEMA_VERSION
                    + "."
            );
        }

        if (challenge.getProgressMode() == null) {
            throw ChallengeDefinitionValidator.invalidDefinition(
                challenge,
                "The progress mode must not be null."
            );
        }

        if (conditionsJson == null || conditionsJson.isBlank()) {
            throw ChallengeDefinitionValidator.invalidDefinition(
                challenge,
                "The conditions JSON must not be blank."
            );
        }
    }
}
