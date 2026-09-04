package io.github.thomashtn.valoquests.challenge.parser;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.exception.InvalidChallengeDefinitionException;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScope;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
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

        validateDefinition(challenge, definition);

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
            throw invalidDefinition(
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
            throw invalidDefinition(
                challenge,
                "The challenge code must not be blank."
            );
        }

        if (challenge.getSchemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            throw invalidDefinition(
                challenge,
                "Unsupported schema version "
                    + challenge.getSchemaVersion()
                    + ". Expected "
                    + SUPPORTED_SCHEMA_VERSION
                    + "."
            );
        }

        if (challenge.getProgressMode() == null) {
            throw invalidDefinition(
                challenge,
                "The progress mode must not be null."
            );
        }

        if (conditionsJson == null || conditionsJson.isBlank()) {
            throw invalidDefinition(
                challenge,
                "The conditions JSON must not be blank."
            );
        }
    }

    /**
     * Validates the parsed conditions and their compatibility with the selected
     * progress mode.
     *
     * @param challenge  persisted challenge
     * @param definition parsed definition
     */
    private void validateDefinition(
        Challenge challenge,
        ChallengeDefinition definition
    ) {
        for (ChallengeCondition condition : definition.conditions()) {
            validateCondition(challenge, condition);
        }

        validateConditionCount(challenge, definition);
        validateProgressMode(challenge, definition);
    }

    /**
     * Validates the mandatory attributes of one condition.
     *
     * @param challenge owning challenge
     * @param condition condition to validate
     */
    private void validateCondition(
        Challenge challenge,
        ChallengeCondition condition
    ) {
        if (condition == null) {
            throw invalidDefinition(
                challenge,
                "A challenge condition must not be null."
            );
        }

        if (condition.metric() == null) {
            throw invalidDefinition(
                challenge,
                "Every condition must define a metric."
            );
        }

        if (condition.operator() == null) {
            throw invalidDefinition(
                challenge,
                "Every condition must define an operator."
            );
        }

        if (condition.target() == null
            || condition.target().signum() < 0) {
            throw invalidDefinition(
                challenge,
                "Every condition must define a non-negative target."
            );
        }
    }

    /**
     * Verifies whether the number of conditions matches the rule structure.
     *
     * @param challenge  persisted challenge
     * @param definition parsed definition
     */
    private void validateConditionCount(
        Challenge challenge,
        ChallengeDefinition definition
    ) {
        // ALL is the only mode that combines conditions, so it is the only one taking more than one.
        boolean combining = definition.progressMode() == ProgressMode.ALL;

        if (combining && definition.conditions().size() < 2) {
            throw invalidDefinition(
                challenge,
                "An ALL challenge must contain at least two conditions."
            );
        }

        if (!combining && definition.conditions().size() != 1) {
            throw invalidDefinition(
                challenge,
                "A challenge that is not an ALL challenge must contain exactly one condition."
            );
        }
    }

    /**
     * Verifies the attributes required by each progress mode.
     *
     * @param challenge  persisted challenge
     * @param definition parsed definition
     */
    private void validateProgressMode(
        Challenge challenge,
        ChallengeDefinition definition
    ) {
        switch (definition.progressMode()) {
            case SUM -> validateSum(challenge, definition);
            case DISTINCT_COUNT, MAX_GROUP -> validateGrouped(challenge, definition);
            case COUNT_MATCHES -> validateOccurrences(challenge, definition);
            case MAX_STREAK -> validateStreak(challenge, definition);
            case RATIO -> validateRatio(challenge, definition);
            case BASELINE -> validateBaseline(challenge, definition);
            // ALL delegates every condition to the mode each one declares, so it constrains nothing of
            // its own beyond the condition count already checked by validateConditionCount.
            case ALL -> { }
        }
    }

    /**
     * Validates a summed challenge definition.
     *
     * @param challenge  persisted challenge
     * @param definition parsed definition
     */
    private void validateSum(
        Challenge challenge,
        ChallengeDefinition definition
    ) {
        ChallengeCondition condition = definition.singleCondition();

        if (condition.groupBy() != null) {
            throw invalidDefinition(
                challenge,
                "SUM conditions must not define groupBy."
            );
        }
    }

    /**
     * Validates a grouped challenge definition.
     *
     * @param challenge  persisted challenge
     * @param definition parsed definition
     */
    private void validateGrouped(
        Challenge challenge,
        ChallengeDefinition definition
    ) {
        ChallengeCondition condition = definition.singleCondition();

        if (condition.groupBy() == null) {
            throw invalidDefinition(
                challenge,
                definition.progressMode()
                    + " requires a groupBy value."
            );
        }
    }

    /**
     * Validates a match-occurrence challenge definition.
     *
     * @param challenge  persisted challenge
     * @param definition parsed definition
     */
    private void validateOccurrences(
        Challenge challenge,
        ChallengeDefinition definition
    ) {
        ChallengeCondition condition = definition.singleCondition();

        if (condition.scope() != ChallengeScope.PER_MATCH) {
            throw invalidDefinition(
                challenge,
                "COUNT_MATCHES requires the PER_MATCH scope."
            );
        }

        if (condition.occurrences() == null
            || condition.occurrences() <= 0) {
            throw invalidDefinition(
                challenge,
                "COUNT_MATCHES requires a positive occurrences value."
            );
        }
    }

    /**
     * Validates a consecutive-match challenge definition.
     *
     * @param challenge  persisted challenge
     * @param definition parsed definition
     */
    private void validateStreak(
        Challenge challenge,
        ChallengeDefinition definition
    ) {
        ChallengeCondition condition = definition.singleCondition();

        if (condition.scope() != ChallengeScope.PER_MATCH) {
            throw invalidDefinition(
                challenge,
                "MAX_STREAK requires the PER_MATCH scope."
            );
        }

        if (condition.streak() == null || condition.streak() <= 0) {
            throw invalidDefinition(
                challenge,
                "MAX_STREAK requires a positive streak value."
            );
        }
    }

    /**
     * Validates a ratio challenge definition.
     *
     * @param challenge  persisted challenge
     * @param definition parsed definition
     */
    private void validateRatio(
        Challenge challenge,
        ChallengeDefinition definition
    ) {
        ChallengeCondition condition = definition.singleCondition();

        if (condition.minimumMatches() != null
            && condition.minimumMatches() <= 0) {
            throw invalidDefinition(
                challenge,
                "minimumMatches must be positive when provided."
            );
        }
    }

    /**
     * Validates a baseline progression challenge definition.
     *
     * <p>The target is an improvement in percent over the player's own baseline, so it has to be
     * strictly positive: a target of zero would be satisfied by standing still, and a negative one by
     * getting worse.
     *
     * @param challenge  persisted challenge
     * @param definition parsed definition
     */
    private void validateBaseline(
        Challenge challenge,
        ChallengeDefinition definition
    ) {
        ChallengeCondition condition = definition.singleCondition();

        if (condition.target() == null || condition.target().signum() <= 0) {
            throw invalidDefinition(
                challenge,
                "BASELINE requires a positive improvement target, in percent."
            );
        }

        if (condition.minimumMatches() == null
            || condition.minimumMatches() <= 0) {
            throw invalidDefinition(
                challenge,
                "BASELINE requires a positive minimumMatches value."
            );
        }
    }

    /**
     * Creates a contextual validation exception.
     *
     * @param challenge invalid challenge
     * @param message   validation message
     * @return contextual exception
     */
    private InvalidChallengeDefinitionException invalidDefinition(
        Challenge challenge,
        String message
    ) {
        return new InvalidChallengeDefinitionException(
            buildErrorMessage(challenge, message)
        );
    }

    /**
     * Creates a contextual parsing exception.
     *
     * @param challenge invalid challenge
     * @param message   validation message
     * @param cause     parsing failure
     * @return contextual exception
     */
    private InvalidChallengeDefinitionException invalidDefinition(
        Challenge challenge,
        String message,
        Throwable cause
    ) {
        return new InvalidChallengeDefinitionException(
            buildErrorMessage(challenge, message),
            cause
        );
    }

    /**
     * Builds an error message containing the challenge identifier.
     *
     * @param challenge invalid challenge
     * @param message   validation message
     * @return contextual error message
     */
    private String buildErrorMessage(
        Challenge challenge,
        String message
    ) {
        String challengeCode = challenge.getCode() == null
            ? "<unknown>"
            : challenge.getCode();

        return "Invalid challenge definition ["
            + challengeCode
            + "]: "
            + message;
    }
}
